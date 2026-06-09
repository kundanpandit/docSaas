package com.docsaas.api.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import com.docsaas.api.enums.ProcessingStatus;
import com.docsaas.api.enums.ToolType;

import lombok.Data;

@Data
@Table("processing_history")
public class ProcessingHistory {

    @Id
    private Long id;

    private Long userId;
    private ToolType toolType;
    private Long inputFileId;
    private Long outputFileId;
    private ProcessingStatus status;
    private Long processingTimeMs;
    private Integer retryCount;
    private String errorMessage;
    private LocalDateTime createdAt;
}
