package com.docsaas.api.dto;

import lombok.Data;

@Data
public class PdfSummaryRequestDTO {
    private Long fileId;
    private String summaryType; // SHORT, DETAILED
}
