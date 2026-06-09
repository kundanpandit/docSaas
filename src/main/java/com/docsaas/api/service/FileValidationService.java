package com.docsaas.api.service;

import org.springframework.stereotype.Service;

import com.docsaas.api.model.Document;
import com.docsaas.api.repository.DocumentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FileValidationService {

    private final DocumentRepository documentRepository;
    private final AuthService authService; 

    public Document validateUserFile(Long fileId) {

        Long userId = authService.getLoggedInUserId();

        Document doc = documentRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));

        if (!doc.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to this file");
        }

        if (!doc.getFileType().equalsIgnoreCase("application/pdf")) {
            throw new RuntimeException("Only PDF files are allowed");
        }

        return doc;
    }
}
