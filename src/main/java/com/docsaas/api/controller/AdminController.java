package com.docsaas.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.docsaas.api.response.ApiResponse;
import com.docsaas.api.service.UserService;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')") // Class-level security
public class AdminController {

    private final UserService service;

    public AdminController(UserService service) {
        this.service = service;
    }

    // Simple admin health check
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<?>> adminHealth() {
        return ResponseEntity.ok(ApiResponse.success("Admin access verified"));
    }

    // Promote user to ADMIN
    @PatchMapping("/users/{id}/promote")
    public ResponseEntity<ApiResponse<?>> promoteUser(@PathVariable Long id) {

        service.promoteToAdmin(id);

        return ResponseEntity.ok(
                ApiResponse.success("User promoted to ADMIN successfully")
        );
    }
}
