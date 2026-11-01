package com.docsaas.api.jobs;

import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.springframework.stereotype.Component;

import com.docsaas.api.service.JobRunner;

/**
 * Runs on any instance with the JobRunr background server enabled. If this throws, JobRunr
 * retries with exponential back-off; JobRunner only throws for transient failures and keeps
 * its own attempt count, so a job is never retried more than docsaas.jobs.max-attempts times.
 */
@Component
public class ProcessDocumentJobHandler implements JobRequestHandler<ProcessDocumentJob> {

    private final JobRunner jobRunner;

    public ProcessDocumentJobHandler(JobRunner jobRunner) {
        this.jobRunner = jobRunner;
    }

    @Override
    @Job(name = "Process document job %0", retries = 2)
    public void run(ProcessDocumentJob request) throws Exception {
        jobRunner.run(request.jobId());
    }
}
