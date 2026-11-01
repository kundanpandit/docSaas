package com.docsaas.api.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.docsaas.api.enums.FileKind;
import com.docsaas.api.exception.ApiException;
import com.docsaas.api.model.Document;
import com.docsaas.api.processing.FileTypeDetector;
import com.docsaas.api.repository.DocumentRepository;



@Service
public class DocumentService {

    private final DocumentRepository repo;
    private final StorageService storage;

    public DocumentService(DocumentRepository repo, StorageService storage) {
        this.repo = repo;
        this.storage = storage;
    }

    public Document upload(MultipartFile file, Long userId) throws IOException {

        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("EMPTY_FILE", "Please select a non-empty file");
        }

        // 1. Validate type from content, not from the client-supplied content type
        FileKind kind;
        try (InputStream in = file.getInputStream()) {
            kind = FileTypeDetector.detect(in)
                    .orElseThrow(() -> ApiException.badRequest("UNSUPPORTED_FILE_TYPE",
                            "Only PDF, PNG and JPEG files are supported"));
        }

        // 2. Server-generated storage key; the original filename is only kept as metadata
        String storedName = UUID.randomUUID().toString();
        String key = "documents/" + userId + "/" + storedName;

        // 3. Save file to disk
        try (InputStream in = file.getInputStream()) {
            storage.save(key, in);
        }

        // 4. Create Document object
        Document doc = new Document();
        doc.setUserId(userId);
        doc.setOriginalName(file.getOriginalFilename());
        doc.setStoredName(storedName);
        doc.setFileType(kind.getMimeType());
        doc.setFileSize(file.getSize());
        doc.setStoragePath(key);
        doc.setStatus("ACTIVE");
        doc.setCreatedAt(LocalDateTime.now());
        doc.setUpdatedAt(LocalDateTime.now());

        // 5. Save to DB
        return repo.save(doc);
    }

    public List<Document> getUserDocuments(Long userId) {
        return repo.findByUserId(userId);
    }


    // for download
    public ResponseEntity<InputStreamResource> downloadDocument(Long docId, Long userId) throws IOException {

        Document doc = findOwned(docId, userId);

        Path file;
        try {
            file = storage.resolve(doc.getStoragePath());
        } catch (IllegalArgumentException e) {
            // legacy rows stored absolute paths outside the storage root
            throw ApiException.notFound("Document file not found");
        }
        if (!Files.exists(file)) {
            throw ApiException.notFound("Document file not found");
        }

        InputStreamResource resource = new InputStreamResource(Files.newInputStream(file));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(doc.getOriginalName(), StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(Files.size(file))
                .body(resource);

    }

    // for deletion
    public void deleteDocument(Long docId, Long userId) {

        Document doc = findOwned(docId, userId);

        try {
            storage.deleteQuietly(doc.getStoragePath());
        } catch (IllegalArgumentException e) {
            // legacy absolute path: nothing under our storage root to delete
        }

        repo.deleteById(docId);
    }

    /** Unknown and foreign documents both return 404 so ids cannot be probed. */
    private Document findOwned(Long docId, Long userId) {
        Document doc = repo.findById(docId)
                .orElseThrow(() -> ApiException.notFound("Document not found"));

        if (doc.getUserId() == null || !doc.getUserId().equals(userId)) {
            throw ApiException.notFound("Document not found");
        }
        return doc;
    }


}
