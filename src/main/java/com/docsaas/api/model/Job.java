package com.docsaas.api.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import com.docsaas.api.enums.JobStatus;
import com.docsaas.api.enums.ToolCode;

import lombok.Data;

@Data
@Table("jobs")
public class Job {

    @Id
    private Long id;

    private String publicId;
    private Long userId;
    private String clientIp;
    private ToolCode toolCode;
    private JobStatus status;
    private String optionsJson;
    private String errorCode;
    private String errorMessage;
    private String resultSummary;
    /** Processing attempts so far; unexpected failures are retried up to docsaas.jobs.max-attempts. */
    private Integer attempts;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime expiresAt;
}
