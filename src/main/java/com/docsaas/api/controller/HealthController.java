package com.docsaas.api.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.docsaas.api.dto.UploadRequest;
import com.docsaas.api.response.ApiResponse;

import jakarta.validation.Valid;

@RestController
public class HealthController {

    @GetMapping("/api/v1/health")
    public ApiResponse<Map<String, Object>> health() {

        Map<String, Object> data = new HashMap<>();
        data.put("status", "UP");
        data.put("service", "DocSaaS API");
        data.put("time", LocalDateTime.now());

        return new ApiResponse<>(true, "Health check success", data);
    }
    
    @GetMapping("/api/v1/test-error")
    public ApiResponse<Object> testError() {
        throw new RuntimeException("Test error from DocSaaS");
    }
    
    @PostMapping("/api/v1/validate-test")
    public ApiResponse<String> validateTest(@Valid @RequestBody UploadRequest request) {
        return new ApiResponse<>(true, "Validation passed", "OK");
    }

}
