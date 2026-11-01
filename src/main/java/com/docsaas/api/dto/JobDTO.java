package com.docsaas.api.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Public view of a job. Never exposes storage paths or internal ids. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record JobDTO(
        String id,
        String tool,
        String status,
        String errorCode,
        String errorMessage,
        String resultSummary,
        Output output,
        LocalDateTime createdAt,
        LocalDateTime completedAt,
        LocalDateTime expiresAt) {

    public record Output(String fileName, String mimeType, Long sizeBytes) {
    }
}
