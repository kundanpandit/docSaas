package com.docsaas.api.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.docsaas.api.enums.FileKind;
import com.docsaas.api.enums.JobStatus;
import com.docsaas.api.enums.ToolCode;
import com.docsaas.api.model.Job;
import com.docsaas.api.model.JobFile;
import com.docsaas.api.processing.ProcessingContext;
import com.docsaas.api.processing.ProcessingException;
import com.docsaas.api.processing.ProcessingResult;
import com.docsaas.api.processing.ToolProcessor;
import com.docsaas.api.processing.engine.AiClient.AiException;
import com.docsaas.api.repository.JobFileRepository;
import com.docsaas.api.repository.JobRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Executes one job (called by the JobRunr handler on a worker). Logs lifecycle events,
 * never document contents.
 *
 * Failure policy:
 * - ProcessingException (bad input/options): fail immediately, never retried.
 * - Transient errors (AI rate limit/outage, LibreOffice busy, I/O, crashes): rethrown so JobRunr
 *   retries with back-off, until docsaas.jobs.max-attempts is reached; then fail cleanly.
 * Inputs are kept until the job reaches a final state, so retries have something to work on.
 */
@Service
public class JobRunner {

    private static final Logger log = LoggerFactory.getLogger(JobRunner.class);

    private final JobRepository jobRepository;
    private final JobFileRepository jobFileRepository;
    private final StorageService storage;
    private final ObjectMapper objectMapper;
    private final CreditService credits;
    private final Map<ToolCode, ToolProcessor> processors = new EnumMap<>(ToolCode.class);

    @Value("${docsaas.jobs.max-attempts:3}")
    private int maxAttempts;

    public JobRunner(JobRepository jobRepository, JobFileRepository jobFileRepository, StorageService storage,
            ObjectMapper objectMapper, CreditService credits, List<ToolProcessor> processorList) {
        this.jobRepository = jobRepository;
        this.jobFileRepository = jobFileRepository;
        this.storage = storage;
        this.objectMapper = objectMapper;
        this.credits = credits;
        processorList.forEach(p -> processors.put(p.tool(), p));
    }

    public void run(Long jobId) throws Exception {
        Job job = jobRepository.findById(jobId).orElse(null);
        // Already finished (e.g. duplicate delivery after a worker crash): nothing to do.
        if (job == null || job.getStatus().isTerminal()) {
            return;
        }
        String dir = JobService.jobDir(job);
        long started = System.currentTimeMillis();
        int attempt = (job.getAttempts() == null ? 0 : job.getAttempts()) + 1;

        job.setStatus(JobStatus.PROCESSING);
        job.setAttempts(attempt);
        job.setStartedAt(LocalDateTime.now());
        job = jobRepository.save(job);

        boolean finished = true;
        try {
            List<ProcessingContext.InputFile> inputs = jobFileRepository
                    .findByJobIdAndRoleOrderBySortOrder(job.getId(), JobFile.ROLE_INPUT).stream()
                    .map(f -> new ProcessingContext.InputFile(storage.resolve(f.getStorageKey()),
                            f.getOriginalName(), kindOf(f.getMimeType())))
                    .toList();
            if (inputs.isEmpty() || inputs.stream().anyMatch(i -> !Files.exists(i.path()))) {
                throw new ProcessingException("INPUT_MISSING", "The uploaded file is no longer available. Please upload it again.");
            }

            // Fresh work directory per attempt
            storage.deleteTreeQuietly(dir + "/work");
            Path workDir = Files.createDirectories(storage.resolve(dir + "/work"));
            Map<String, String> options = job.getOptionsJson() == null ? Map.of()
                    : objectMapper.readValue(job.getOptionsJson(), new TypeReference<Map<String, String>>() {});

            ToolProcessor processor = processors.get(job.getToolCode());
            String unavailable = processor.unavailableReason();
            if (unavailable != null) {
                throw new ProcessingException("ENGINE_UNAVAILABLE", unavailable);
            }
            Path data = jobFileRepository.findByJobIdAndRoleOrderBySortOrder(job.getId(), JobFile.ROLE_DATA).stream()
                    .findFirst().map(df -> storage.resolve(df.getStorageKey())).orElse(null);
            ProcessingResult result = processor.process(new ProcessingContext(inputs, options, workDir, data));

            String outKey = dir + "/out/result";
            Path outPath = storage.resolve(outKey);
            Files.createDirectories(outPath.getParent());
            Files.move(result.file(), outPath, StandardCopyOption.REPLACE_EXISTING);

            JobFile output = new JobFile();
            output.setJobId(job.getId());
            output.setRole(JobFile.ROLE_OUTPUT);
            output.setSortOrder(0);
            output.setStorageKey(outKey);
            output.setOriginalName(result.fileName());
            output.setMimeType(result.mimeType());
            output.setSizeBytes(Files.size(outPath));
            output.setCreatedAt(LocalDateTime.now());
            jobFileRepository.save(output);

            job.setStatus(JobStatus.SUCCEEDED);
            job.setResultSummary(truncate(result.summary(), 500));
            job.setErrorCode(null);
            job.setErrorMessage(null);
            job.setCompletedAt(LocalDateTime.now());
            jobRepository.save(job);
            log.info("Job {} succeeded: tool={} attempt={} durationMs={}", job.getPublicId(), job.getToolCode(),
                    attempt, System.currentTimeMillis() - started);

        } catch (ProcessingException e) {
            fail(job, e.getCode(), e.getMessage());
            log.info("Job {} rejected: tool={} code={}", job.getPublicId(), job.getToolCode(), e.getCode());

        } catch (AiException e) {
            if (e.isRetryable() && attempt < maxAttempts) {
                finished = false;
                requeue(job, e.getMessage());
                log.warn("Job {} AI call failed (attempt {}/{}), will retry", job.getPublicId(), attempt, maxAttempts);
                throw e;
            }
            fail(job, "AI_UNAVAILABLE", e.getMessage());
            log.warn("Job {} failed: AI unavailable", job.getPublicId());

        } catch (Exception | OutOfMemoryError e) {
            if (attempt < maxAttempts && !(e instanceof OutOfMemoryError)) {
                finished = false;
                requeue(job, null);
                log.warn("Job {} failed on attempt {}/{}, will retry: {}", job.getPublicId(), attempt, maxAttempts,
                        e.getClass().getSimpleName());
                throw e;
            }
            fail(job, "PROCESSING_FAILED", "We couldn't process this file. Please check it and try again.");
            log.error("Job {} failed: tool={} attempt={}", job.getPublicId(), job.getToolCode(), attempt, e);

        } finally {
            storage.deleteTreeQuietly(dir + "/work");
            if (finished) {
                // Final state reached: uploads are not retained.
                storage.deleteTreeQuietly(dir + "/in");
                scrubSensitiveOptions(job);
            }
        }
    }

    /** Passwords and similar options are only needed while processing; don't keep them in the database. */
    private void scrubSensitiveOptions(Job job) {
        ToolProcessor processor = processors.get(job.getToolCode());
        if (processor == null || processor.sensitiveOptions().isEmpty() || job.getOptionsJson() == null) {
            return;
        }
        try {
            Map<String, String> options = new java.util.LinkedHashMap<>(
                    objectMapper.readValue(job.getOptionsJson(), new TypeReference<Map<String, String>>() {}));
            options.keySet().removeAll(processor.sensitiveOptions());
            job.setOptionsJson(objectMapper.writeValueAsString(options));
            jobRepository.save(job);
        } catch (Exception e) {
            log.warn("Could not scrub options of job {}", job.getPublicId());
        }
    }

    private void requeue(Job job, String note) {
        job.setStatus(JobStatus.QUEUED);
        job.setErrorMessage(note);
        jobRepository.save(job);
    }

    private void fail(Job job, String code, String message) {
        job.setStatus(JobStatus.FAILED);
        job.setErrorCode(code);
        job.setErrorMessage(truncate(message, 500));
        job.setCompletedAt(LocalDateTime.now());
        jobRepository.save(job);
        credits.refundIfFailed(job.getId());
    }

    private static String truncate(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }

    private static FileKind kindOf(String mimeType) {
        for (FileKind kind : FileKind.values()) {
            if (kind.getMimeType().equals(mimeType)) {
                return kind;
            }
        }
        throw new IllegalStateException("Unknown stored mime type");
    }
}
