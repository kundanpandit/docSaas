package com.docsaas.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JobResponseDTO {
    private Long jobId;
    private String status;
    private String message;
}
