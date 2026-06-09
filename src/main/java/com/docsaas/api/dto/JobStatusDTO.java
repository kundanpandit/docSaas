package com.docsaas.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JobStatusDTO {
    private Long jobId;
    private String toolType;
    private String status;
    private String downloadUrl;
    private String errorMessage;
}
