package com.docsaas.api.controller;

import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import com.docsaas.api.dto.LoginRequest;
import com.docsaas.api.response.ApiResponse;
import com.docsaas.api.service.UserService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService service;

    public AuthController(UserService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public ApiResponse<?> login(@RequestBody @Valid LoginRequest req) {

        return ApiResponse.success(
                service.login(req.getEmail(), req.getPassword())
        );
    }
}
