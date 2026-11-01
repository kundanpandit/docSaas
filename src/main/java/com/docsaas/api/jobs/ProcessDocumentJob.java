package com.docsaas.api.jobs;

import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.jobs.lambdas.JobRequestHandler;

/**
 * JobRunr job payload: only our job id, so it serializes trivially and survives code changes.
 * Must stay a simple record (JobRunr stores it as JSON in the jobrunr_* tables).
 */
public record ProcessDocumentJob(Long jobId) implements JobRequest {

    @Override
    public Class<? extends JobRequestHandler> getJobRequestHandler() {
        return ProcessDocumentJobHandler.class;
    }
}
