package com.docsaas.api.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.docsaas.api.dto.ChatDTO;
import com.docsaas.api.response.ApiResponse;
import com.docsaas.api.service.ChatService;
import com.docsaas.api.service.JobService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {

    private final JobService jobService;
    private final ChatService chatService;

    public JobController(JobService jobService, ChatService chatService) {
        this.jobService = jobService;
        this.chatService = chatService;
    }

    /** Recent jobs of the logged-in user (workspace history). */
    @GetMapping
    public ApiResponse<?> myJobs() {
        return ApiResponse.success(jobService.myRecentJobs());
    }

    @GetMapping("/{jobId}")
    public ApiResponse<?> getJob(@PathVariable String jobId) {
        return ApiResponse.success(jobService.getJob(jobId));
    }

    /** Ask a question about a document prepared by the chat-with-pdf tool. */
    @PostMapping("/{jobId}/chat")
    public ApiResponse<ChatDTO.Answer> chat(@PathVariable String jobId, @RequestBody @Valid ChatDTO.Request request,
            HttpServletRequest httpRequest) {
        return ApiResponse.success(chatService.ask(jobId, request, httpRequest.getRemoteAddr()));
    }

    @GetMapping("/{jobId}/download")
    public ResponseEntity<Resource> download(@PathVariable String jobId) {
        JobService.Download d = jobService.getDownload(jobId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(d.fileName(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType(d.mimeType()))
                .contentLength(d.size())
                .body(new FileSystemResource(d.path()));
    }
}
