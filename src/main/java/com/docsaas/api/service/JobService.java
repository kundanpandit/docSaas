package com.docsaas.api.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.docsaas.api.dto.JobResponseDTO;
import com.docsaas.api.dto.JobStatusDTO;
import com.docsaas.api.enums.ProcessingStatus;
import com.docsaas.api.enums.ToolType;
import com.docsaas.api.model.ProcessingHistory;
import com.docsaas.api.repository.ProcessingHistoryRepository;
import com.docsaas.api.security.SecurityUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JobService {

	private final AsyncJobProcessor asyncJobProcessor;
	
    private final ProcessingHistoryRepository repository;

    private final FileValidationService fileValidationService;
    @Transactional
    public JobResponseDTO createJob(String tool, Long fileId) {

        // 🔒 Validate ownership + type
        fileValidationService.validateUserFile(fileId);

        ProcessingHistory job = new ProcessingHistory();
        job.setToolType(ToolType.valueOf(tool));
        job.setInputFileId(fileId);
        job.setStatus(ProcessingStatus.PENDING);
        job.setCreatedAt(LocalDateTime.now());
        job.setUserId(SecurityUtil.getLoggedInUserId());


        job = repository.save(job);

        asyncJobProcessor.processJob(job.getId());
        
        return new JobResponseDTO(job.getId(), "PENDING", tool + " job started");
    }


    public JobStatusDTO getJobStatus(Long jobId) {

        ProcessingHistory job = repository.findById(jobId).orElseThrow();

        return new JobStatusDTO(
                job.getId(),
                job.getToolType().name(),
                job.getStatus().name(),
                null,
                job.getErrorMessage()
        );
    }
}
