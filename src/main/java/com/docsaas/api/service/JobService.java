package com.docsaas.api.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.docsaas.api.dto.JobDTO;
import com.docsaas.api.enums.FileKind;
import com.docsaas.api.enums.JobStatus;
import com.docsaas.api.enums.ToolCode;
import com.docsaas.api.exception.ApiException;
import com.docsaas.api.jobs.ProcessDocumentJob;
import com.docsaas.api.model.Job;
import com.docsaas.api.model.JobFile;
import com.docsaas.api.processing.FileTypeDetector;
import com.docsaas.api.processing.ProcessingException;
import com.docsaas.api.processing.ToolProcessor;
import com.docsaas.api.repository.JobFileRepository;
import com.docsaas.api.repository.JobRepository;
import com.docsaas.api.security.SecurityUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class JobService {

    private static final Logger log = LoggerFactory.getLogger(JobService.class);

    private static final int MAX_OPTIONS = 10;
    private static final int MAX_OPTION_LENGTH = 200;

    private final JobRepository jobRepository;
    private final JobFileRepository jobFileRepository;
    private final StorageService storage;
    private final JobRequestScheduler jobScheduler;
    private final RateLimiter rateLimiter;
    private final CreditService credits;
    private final ObjectMapper objectMapper;
    private final Map<ToolCode, ToolProcessor> processors = new EnumMap<>(ToolCode.class);

    @Value("${docsaas.retention.minutes}")
    private long retentionMinutes;

    @Value("${docsaas.limits.guest-jobs-per-hour:30}")
    private int guestJobsPerHour;

    @Value("${docsaas.limits.user-jobs-per-hour:200}")
    private int userJobsPerHour;

    public JobService(JobRepository jobRepository, JobFileRepository jobFileRepository, StorageService storage,
            JobRequestScheduler jobScheduler, RateLimiter rateLimiter, CreditService credits,
            ObjectMapper objectMapper, List<ToolProcessor> processorList) {
        this.jobRepository = jobRepository;
        this.jobFileRepository = jobFileRepository;
        this.storage = storage;
        this.jobScheduler = jobScheduler;
        this.rateLimiter = rateLimiter;
        this.credits = credits;
        this.objectMapper = objectMapper;
        processorList.forEach(p -> processors.put(p.tool(), p));
    }

    /** Every implemented tool with its current availability (null reason = ready). */
    public Map<ToolCode, String> toolStatus() {
        Map<ToolCode, String> status = new EnumMap<>(ToolCode.class);
        processors.forEach((tool, p) -> status.put(tool, p.unavailableReason()));
        return status;
    }

    public JobDTO createJob(String toolSlug, List<MultipartFile> files, Map<String, String> rawOptions,
            String clientIp) {
        return createJob(toolSlug, files, rawOptions, null, clientIp);
    }

    /** @param data optional JSON part for tools that take structured input (see ToolProcessor.maxDataBytes) */
    public JobDTO createJob(String toolSlug, List<MultipartFile> files, Map<String, String> rawOptions,
            MultipartFile data, String clientIp) {

        ToolCode tool = ToolCode.fromSlug(toolSlug)
                .filter(processors::containsKey)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "TOOL_NOT_FOUND",
                        "This tool is not available yet"));
        ToolProcessor processor = processors.get(tool);
        Long userId = SecurityUtil.getUserIdOrNull();
        if (userId == null && CreditService.isAi(tool)) {
            credits.checkAllowed(null, clientIp, tool); // throws LOGIN_REQUIRED
        }
        String unavailable = processor.unavailableReason();
        if (unavailable != null) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "ENGINE_UNAVAILABLE", unavailable);
        }

        Map<String, String> options = sanitizeOptions(rawOptions);
        List<FileKind> kinds = validateFiles(tool, files);

        try {
            processor.validateOptions(options);
            processor.validateData(readData(processor, data));
        } catch (ProcessingException e) {
            throw ApiException.badRequest(e.getCode(), e.getMessage());
        }

        // Cheap rejection before the rate limiter counts the attempt; re-checked under the lock below.
        credits.checkAllowed(userId, clientIp, tool);

        String limiterKey = userId != null ? "user:" + userId : "ip:" + clientIp;
        int limit = userId != null ? userJobsPerHour : guestJobsPerHour;
        if (!rateLimiter.tryAcquire(limiterKey, limit)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED",
                    userId != null
                            ? "You have reached the hourly limit. Please try again later."
                            : "You have reached the hourly limit for guests. Log in for a higher limit, or try again later.");
        }

        LocalDateTime now = LocalDateTime.now();
        Job job = new Job();
        job.setPublicId(UUID.randomUUID().toString());
        job.setUserId(userId);
        job.setClientIp(clientIp);
        job.setToolCode(tool);
        job.setStatus(JobStatus.QUEUED);
        job.setAttempts(0);
        job.setOptionsJson(toJson(options));
        job.setCreatedAt(now);
        job.setExpiresAt(now.plusMinutes(retentionMinutes));
        // The saved QUEUED job is what counts as usage, so check-and-save is atomic per visitor.
        synchronized (credits.lockFor(userId, clientIp)) {
            credits.checkAllowed(userId, clientIp, tool);
            job = jobRepository.save(job);
        }

        try {
            for (int i = 0; i < files.size(); i++) {
                MultipartFile file = files.get(i);
                String key = jobDir(job) + "/in/" + i;
                try (InputStream in = file.getInputStream()) {
                    storage.save(key, in);
                }
                JobFile jf = new JobFile();
                jf.setJobId(job.getId());
                jf.setRole(JobFile.ROLE_INPUT);
                jf.setSortOrder(i);
                jf.setStorageKey(key);
                jf.setOriginalName(truncate(file.getOriginalFilename(), 255));
                jf.setMimeType(kinds.get(i).getMimeType());
                jf.setSizeBytes(file.getSize());
                jf.setCreatedAt(now);
                jobFileRepository.save(jf);
            }
            if (data != null && !data.isEmpty()) {
                String key = jobDir(job) + "/in/data.json";
                try (InputStream in = data.getInputStream()) {
                    storage.save(key, in);
                }
                JobFile jf = new JobFile();
                jf.setJobId(job.getId());
                jf.setRole(JobFile.ROLE_DATA);
                jf.setSortOrder(0);
                jf.setStorageKey(key);
                jf.setOriginalName("data.json");
                jf.setMimeType("application/json");
                jf.setSizeBytes(data.getSize());
                jf.setCreatedAt(now);
                jobFileRepository.save(jf);
            }
        } catch (IOException e) {
            failAndCleanup(job, "UPLOAD_FAILED", "Upload could not be saved. Please try again.");
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "UPLOAD_FAILED",
                    "Upload could not be saved. Please try again.");
        }

        // Persisted in MySQL by JobRunr: survives restarts and is picked up by any worker instance.
        // The JobRunr id is derived from our job id, so enqueueing twice can never run it twice.
        try {
            jobScheduler.enqueue(UUID.nameUUIDFromBytes(job.getPublicId().getBytes()), new ProcessDocumentJob(job.getId()));
        } catch (RuntimeException e) {
            log.error("Could not enqueue job {}", job.getPublicId(), e);
            failAndCleanup(job, "SERVER_BUSY", "The server is busy. Please try again in a minute.");
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "SERVER_BUSY",
                    "The server is busy. Please try again in a minute.");
        }

        credits.chargeJob(job);

        log.info("Job {} created: tool={} files={} user={}", job.getPublicId(), tool, files.size(),
                userId != null ? userId : "guest");
        return toDto(job);
    }

    public JobDTO getJob(String publicId) {
        return toDto(findAuthorized(publicId));
    }

    public List<JobDTO> myRecentJobs() {
        Long userId = SecurityUtil.getLoggedInUserId();
        return jobRepository.findRecentByUserId(userId, 50).stream().map(this::toDto).toList();
    }

    public record Download(Path path, String fileName, String mimeType, long size) {
    }

    public Download getDownload(String publicId) {
        Job job = findAuthorized(publicId);
        if (job.getStatus() == JobStatus.EXPIRED) {
            throw new ApiException(HttpStatus.GONE, "JOB_EXPIRED",
                    "This file has expired and was deleted. Please run the tool again.");
        }
        if (job.getStatus() != JobStatus.SUCCEEDED) {
            throw new ApiException(HttpStatus.CONFLICT, "JOB_NOT_READY", "The result is not ready yet");
        }
        JobFile output = findOutput(job)
                .orElseThrow(() -> new ApiException(HttpStatus.GONE, "JOB_EXPIRED", "The result is no longer available"));
        Path path = storage.resolve(output.getStorageKey());
        if (!Files.exists(path)) {
            throw new ApiException(HttpStatus.GONE, "JOB_EXPIRED", "The result is no longer available");
        }
        return new Download(path, output.getOriginalName(), output.getMimeType(), output.getSizeBytes());
    }

    /** Text index of a finished chat-with-pdf job, with the same access rules as downloads. */
    public Path chatIndexFile(String publicId) {
        Job job = findAuthorized(publicId);
        if (job.getToolCode() != ToolCode.CHAT_WITH_PDF) {
            throw ApiException.badRequest("NOT_A_CHAT", "This job is not a chat document");
        }
        return getDownload(publicId).path();
    }

    // ===== helpers =====

    /**
     * Guest jobs are reachable by anyone holding the random job id (the id is the capability).
     * Jobs created while logged in are only visible to their owner. Unknown and foreign jobs
     * both return 404 so ids cannot be probed.
     */
    private Job findAuthorized(String publicId) {
        Job job = jobRepository.findByPublicId(publicId)
                .orElseThrow(() -> ApiException.notFound("Job not found"));
        if (job.getUserId() != null && !job.getUserId().equals(SecurityUtil.getUserIdOrNull())) {
            throw ApiException.notFound("Job not found");
        }
        return job;
    }

    private Optional<JobFile> findOutput(Job job) {
        return jobFileRepository.findByJobIdAndRoleOrderBySortOrder(job.getId(), JobFile.ROLE_OUTPUT)
                .stream().findFirst();
    }

    private List<FileKind> validateFiles(ToolCode tool, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw ApiException.badRequest("NO_FILES", "Please select a file to upload");
        }
        if (files.size() < tool.getMinFiles()) {
            throw ApiException.badRequest("INVALID_FILE_COUNT",
                    "Please select at least " + tool.getMinFiles() + " files");
        }
        if (files.size() > tool.getMaxFiles()) {
            throw ApiException.badRequest("INVALID_FILE_COUNT",
                    "You can upload at most " + tool.getMaxFiles() + (tool.getMaxFiles() == 1 ? " file" : " files"));
        }

        List<FileKind> kinds = new ArrayList<>();
        for (MultipartFile file : files) {
            String name = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
            if (file.isEmpty()) {
                throw ApiException.badRequest("EMPTY_FILE", "\"" + name + "\" is empty");
            }
            Optional<FileKind> kind;
            try (InputStream in = file.getInputStream()) {
                kind = FileTypeDetector.detect(in);
            } catch (IOException e) {
                throw ApiException.badRequest("INVALID_UPLOAD", "\"" + name + "\" could not be read");
            }
            if (kind.isPresent() && kind.get() == FileKind.DOCX && !isWordDocument(file)) {
                // A ZIP that is not a Word document (e.g. .xlsx or a plain archive)
                kind = Optional.empty();
            }
            if (kind.isEmpty() || !tool.getAcceptedKinds().contains(kind.get())) {
                throw ApiException.badRequest("UNSUPPORTED_FILE_TYPE",
                        "\"" + name + "\" is not a supported file. Accepted: " + describe(tool));
            }
            kinds.add(kind.get());
        }
        return kinds;
    }

    /** Parses the data part, enforcing that only tools that take data get it, within their size limit. */
    private com.fasterxml.jackson.databind.JsonNode readData(ToolProcessor processor, MultipartFile data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        if (processor.maxDataBytes() == 0) {
            throw ApiException.badRequest("UNEXPECTED_DATA", "This tool does not take extra data");
        }
        if (data.getSize() > processor.maxDataBytes()) {
            throw ApiException.badRequest("DATA_TOO_LARGE", "Too many changes in one go. Please save in smaller steps.");
        }
        try (InputStream in = data.getInputStream()) {
            com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(in);
            if (node == null || !node.isObject()) {
                throw ApiException.badRequest("INVALID_DATA", "The edit data could not be read");
            }
            return node;
        } catch (IOException e) {
            throw ApiException.badRequest("INVALID_DATA", "The edit data could not be read");
        }
    }

    private static boolean isWordDocument(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            return FileTypeDetector.isWordDocument(in);
        } catch (IOException e) {
            return false;
        }
    }

    private static String describe(ToolCode tool) {
        return String.join(", ", tool.getAcceptedKinds().stream().map(FileKind::name).toList());
    }

    private Map<String, String> sanitizeOptions(Map<String, String> raw) {
        Map<String, String> options = new LinkedHashMap<>();
        if (raw == null) {
            return options;
        }
        raw.forEach((k, v) -> {
            if (k != null && v != null && !k.equals("files")) {
                options.put(k, v);
            }
        });
        if (options.size() > MAX_OPTIONS
                || options.entrySet().stream().anyMatch(e -> e.getKey().length() > 50 || e.getValue().length() > MAX_OPTION_LENGTH)) {
            throw ApiException.badRequest("INVALID_OPTION", "Too many or too long options");
        }
        return options;
    }

    private String toJson(Map<String, String> options) {
        try {
            return objectMapper.writeValueAsString(options);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private void failAndCleanup(Job job, String code, String message) {
        job.setStatus(JobStatus.FAILED);
        job.setOptionsJson(null); // never ran: no reason to keep options such as passwords
        job.setErrorCode(code);
        job.setErrorMessage(message);
        job.setCompletedAt(LocalDateTime.now());
        jobRepository.save(job);
        storage.deleteTreeQuietly(jobDir(job));
    }

    static String jobDir(Job job) {
        return "jobs/" + job.getPublicId();
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    private JobDTO toDto(Job job) {
        JobDTO.Output output = null;
        if (job.getStatus() == JobStatus.SUCCEEDED) {
            output = findOutput(job)
                    .map(f -> new JobDTO.Output(f.getOriginalName(), f.getMimeType(), f.getSizeBytes()))
                    .orElse(null);
        }
        return new JobDTO(job.getPublicId(), job.getToolCode().getSlug(), job.getStatus().name(),
                job.getErrorCode(), job.getErrorMessage(), job.getResultSummary(), output,
                job.getCreatedAt(), job.getCompletedAt(), job.getExpiresAt());
    }
}
