package com.docsaas.api.dto;

import lombok.Data;

@Data
public class SplitPdfRequestDTO {
    private Long fileId;
    private int startPage;
    private int endPage;
}
