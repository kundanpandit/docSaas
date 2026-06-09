package com.docsaas.api.controller;

import java.io.IOException;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.docsaas.api.response.ApiResponse;
import com.docsaas.api.security.SecurityUtil;
import com.docsaas.api.service.DocumentService;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService service;

    public DocumentController(DocumentService service) {
        this.service = service;
    }

    @PostMapping("/upload")
    public ApiResponse<?> upload(@RequestParam("file") MultipartFile file) throws Exception {

        Long userId = SecurityUtil.getLoggedInUserId();
        return ApiResponse.success(service.upload(file, userId));
    }
    
    @GetMapping("/my-files")
    public ApiResponse<?> myFiles() {
        Long userId = SecurityUtil.getLoggedInUserId();
        return ApiResponse.success(service.getUserDocuments(userId));
    }
    
    @GetMapping("/download/{id}")
    public ResponseEntity<InputStreamResource> download(@PathVariable Long id) throws IOException {
        return service.downloadDocument(id);
    }
    
    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable Long id) {

        Long userId = SecurityUtil.getLoggedInUserId();
        service.deleteDocument(id, userId);

        return ApiResponse.success("Document deleted successfully");
    }



}
