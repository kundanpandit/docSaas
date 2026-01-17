package com.docsaas.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

public class UploadRequest {

    @NotBlank(message = "File name is required")
    private String fileName;

    @NotNull(message = "File size is required")
    private Long fileSize;

    public String getFileName() {
        return fileName;
    }

    public Long getFileSize() {
        return fileSize;
    }
}

