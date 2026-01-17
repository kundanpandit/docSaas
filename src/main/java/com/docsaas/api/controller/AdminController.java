package com.docsaas.api.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.docsaas.api.response.ApiResponse;
import com.docsaas.api.service.UserService;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserService service;

    public AdminController(UserService service) {
        this.service = service;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/test")
    public ApiResponse<?> adminTest() {
        return ApiResponse.success("Admin access granted");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/promote/{id}")
    public ApiResponse<?> promoteUser(@PathVariable Long id) {
        service.promoteToAdmin(id);
        return ApiResponse.success("User promoted to ADMIN");
    }
}
