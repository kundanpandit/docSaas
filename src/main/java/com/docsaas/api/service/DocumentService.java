package com.docsaas.api.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.docsaas.api.model.Document;
import com.docsaas.api.repository.DocumentRepository;



@Service
public class DocumentService {

    private final DocumentRepository repo;

    public DocumentService(DocumentRepository repo) {
        this.repo = repo;
    }

    public Document upload(MultipartFile file, Long userId) throws Exception {

        // 1. Generate unique file name
        String storedName = UUID.randomUUID() + "_" + file.getOriginalFilename();

        // 2. Build file path
        Path path = Paths.get("C:/docsaas-storage/" + storedName);

        // 3. Save file to disk
        Files.copy(file.getInputStream(), path);

        // 4. Create Document object
        Document doc = new Document();
        doc.setUserId(userId);
        doc.setOriginalName(file.getOriginalFilename());
        doc.setStoredName(storedName);
        doc.setFileType(file.getContentType());
        doc.setFileSize(file.getSize());
        doc.setStoragePath(path.toString());
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
    public ResponseEntity<InputStreamResource> downloadDocument(Long docId) throws IOException {

        Document doc = repo.findById(docId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        File file = new File(doc.getStoragePath());

        InputStreamResource resource = new InputStreamResource(new FileInputStream(file));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getOriginalName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(file.length())
                .body(resource);

    }
    
    // for deletion
    public void deleteDocument(Long docId, Long userId) {

        Document doc = repo.findById(docId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        // TEMP ownership check
        if (!doc.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized to delete this file");
        }

        File file = new File(doc.getStoragePath());
        if (file.exists()) {
            file.delete();
        }

        repo.deleteById(docId);
    }


}
