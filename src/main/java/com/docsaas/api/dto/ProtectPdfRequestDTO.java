package com.docsaas.api.dto;

import lombok.Data;

@Data
public class ProtectPdfRequestDTO {
    private Long fileId;
    private String password;
}
