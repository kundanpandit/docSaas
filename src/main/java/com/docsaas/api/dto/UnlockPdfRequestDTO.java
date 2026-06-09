package com.docsaas.api.dto;

import lombok.Data;

@Data
public class UnlockPdfRequestDTO {
    private Long fileId;
    private String password;
}
