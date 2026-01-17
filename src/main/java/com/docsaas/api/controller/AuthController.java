package com.docsaas.api.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.docsaas.api.dto.LoginRequest;
import com.docsaas.api.dto.UserResponse;
import com.docsaas.api.response.ApiResponse;
import com.docsaas.api.security.JwtUtil;
import com.docsaas.api.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService service;
    private final JwtUtil jwtUtil;

    public AuthController(UserService service, JwtUtil jwtUtil) {
        this.service = service;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ApiResponse<?> login(@RequestBody @Valid LoginRequest req) {

        UserResponse user = service.login(req.getEmail(), req.getPassword());

        String token = jwtUtil.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole() );

        return ApiResponse.success(
                Map.of("token", token, "user", user )
        );
    }
}

