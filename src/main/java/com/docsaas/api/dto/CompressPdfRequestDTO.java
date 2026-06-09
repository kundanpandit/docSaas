package com.docsaas.api.dto;

import lombok.Data;

@Data
public class CompressPdfRequestDTO {
    private Long fileId;
    private String level; // LOW, MEDIUM, HIGH
}
