package com.docsaas.api.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.docsaas.api.enums.ProcessingStatus;
import com.docsaas.api.model.ProcessingHistory;
import com.docsaas.api.repository.ProcessingHistoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AsyncJobProcessor {

    private final ProcessingHistoryRepository repository;

    @Async("jobExecutor")
    public void processJob(Long jobId) {

        ProcessingHistory job = repository.findById(jobId).orElseThrow();

        try {
            job.setStatus(ProcessingStatus.PROCESSING);
            repository.save(job);

            // Simulate processing delay (real PDF tools in Phase 5B)
            Thread.sleep(3000);

            job.setStatus(ProcessingStatus.SUCCESS);
            repository.save(job);

        } catch (Exception e) {
            job.setStatus(ProcessingStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            repository.save(job);
        }
        System.out.println("Async running on: " + Thread.currentThread().getName());
    }
    
}
