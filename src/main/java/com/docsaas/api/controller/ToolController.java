package com.docsaas.api.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.docsaas.api.dto.JobDTO;
import com.docsaas.api.response.ApiResponse;
import com.docsaas.api.processing.engine.OcrEngine;
import com.docsaas.api.service.CreditService;
import com.docsaas.api.service.JobService;

import jakarta.servlet.http.HttpServletRequest;

/** Public, guest-first tool endpoints (SRS Flow A). */
@RestController
@RequestMapping("/api/v1/tools")
public class ToolController {

    private final JobService jobService;
    private final OcrEngine ocrEngine;
    private final CreditService credits;

    public ToolController(JobService jobService, OcrEngine ocrEngine, CreditService credits) {
        this.jobService = jobService;
        this.ocrEngine = ocrEngine;
        this.credits = credits;
    }

    /** Implemented tools, whether each can run right now, and installed OCR languages. */
    @GetMapping
    public ApiResponse<?> listTools() {
        List<Map<String, Object>> tools = jobService.toolStatus().entrySet().stream()
                .map(e -> {
                    Map<String, Object> t = new java.util.LinkedHashMap<>();
                    t.put("id", e.getKey().getSlug());
                    t.put("available", e.getValue() == null);
                    if (e.getValue() != null) {
                        t.put("reason", e.getValue());
                    }
                    t.put("requiresLogin", CreditService.isAi(e.getKey()));
                    t.put("cost", credits.costOf(e.getKey()));
                    t.put("accepts", e.getKey().getAcceptedKinds().stream().map(k -> k.getMimeType()).toList());
                    t.put("minFiles", e.getKey().getMinFiles());
                    t.put("maxFiles", e.getKey().getMaxFiles());
                    return t;
                })
                .toList();

        List<Map<String, String>> languages = ocrEngine.installedLanguages().stream()
                .map(code -> Map.of("code", code, "name", OcrEngine.LANGUAGE_NAMES.getOrDefault(code, code)))
                .toList();

        return ApiResponse.success(Map.of("tools", tools, "ocrLanguages", languages));
    }

    /**
     * Uploads the files and starts a processing job. Returns 202 with the job;
     * the client polls GET /api/v1/jobs/{id} and then downloads the result.
     * Tool options are sent as additional form fields; tools with structured input (the PDF editor)
     * also take a JSON file part named "data".
     */
    @PostMapping(value = "/{tool}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<JobDTO>> run(
            @PathVariable String tool,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            @RequestParam(value = "data", required = false) MultipartFile data,
            @RequestParam Map<String, String> options,
            HttpServletRequest request) {

        JobDTO job = jobService.createJob(tool, files, options, data, request.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.success(job));
    }
}
