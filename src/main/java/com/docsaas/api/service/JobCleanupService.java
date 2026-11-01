package com.docsaas.api.service;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.docsaas.api.enums.JobStatus;
import com.docsaas.api.model.Job;
import com.docsaas.api.repository.JobRepository;

/**
 * Enforces the retention policy (FR-030) and fails jobs stuck past their deadline.
 * Safe to run on several instances at once: every step is idempotent.
 */
@Service
public class JobCleanupService {

    private static final Logger log = LoggerFactory.getLogger(JobCleanupService.class);

    private final JobRepository jobRepository;
    private final StorageService storage;
    private final RateLimiter rateLimiter;
    private final CreditService credits;
    private final PasswordResetService passwordReset;

    @Value("${docsaas.jobs.stuck-after-minutes:30}")
    private long stuckAfterMinutes;

    public JobCleanupService(JobRepository jobRepository, StorageService storage, RateLimiter rateLimiter,
            CreditService credits, PasswordResetService passwordReset) {
        this.jobRepository = jobRepository;
        this.storage = storage;
        this.rateLimiter = rateLimiter;
        this.credits = credits;
        this.passwordReset = passwordReset;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        purgeExpired();
    }

    @Scheduled(fixedDelayString = "${docsaas.retention.cleanup-interval-ms:300000}",
            initialDelayString = "${docsaas.retention.cleanup-interval-ms:300000}")
    public void purgeExpired() {
        LocalDateTime now = LocalDateTime.now();
        int stuck = jobRepository.failStuck(now, now.minusMinutes(stuckAfterMinutes));
        if (stuck > 0) {
            log.warn("Failed {} jobs that did not finish within {} minutes", stuck, stuckAfterMinutes);
        }
        // Covers jobs failed by the sweeper above and any refund missed by a crash.
        credits.refundAllFailed();
        passwordReset.purgeExpired();

        int count = 0;
        for (Job job : jobRepository.findExpired(now)) {
            storage.deleteTreeQuietly(JobService.jobDir(job));
            job.setStatus(JobStatus.EXPIRED);
            jobRepository.save(job);
            count++;
        }
        if (count > 0) {
            log.info("Expired {} jobs and deleted their files", count);
        }
        rateLimiter.prune();
    }
}
