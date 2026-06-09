package com.docsaas.api.dto;

import lombok.Data;

@Data
public class ConvertPdfRequestDTO {
    private Long fileId;
    private String targetFormat; // WORD, EXCEL, IMAGE
}
