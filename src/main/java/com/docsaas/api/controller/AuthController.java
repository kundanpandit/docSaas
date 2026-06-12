package com.docsaas.api.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.docsaas.api.dto.GoogleTokenRequestDTO;
import com.docsaas.api.dto.LoginRequest;
import com.docsaas.api.dto.RegisterRequestDTO;
import com.docsaas.api.dto.UserResponse;
import com.docsaas.api.response.ApiResponse;
import com.docsaas.api.security.JwtUtil;
import com.docsaas.api.service.GoogleAuthService;
import com.docsaas.api.service.UserService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")

public class AuthController {

    private final UserService service;
    private final JwtUtil jwtUtil;
    private final GoogleAuthService googleAuthService;

    public AuthController(
            UserService service,
            JwtUtil jwtUtil,
            GoogleAuthService googleAuthService) {

        this.service = service;
        this.jwtUtil = jwtUtil;
        this.googleAuthService = googleAuthService;
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
    
    @PostMapping("/register")
    public ApiResponse<?> register(
            @RequestBody @Valid RegisterRequestDTO request) {

        UserResponse user = service.register(request);

        return ApiResponse.success(user);
    }
    
    @PostMapping("/google")
    public ApiResponse<?> googleLogin(
            @RequestBody @Valid GoogleTokenRequestDTO request) {

        GoogleIdToken.Payload payload =
                googleAuthService.verifyToken(
                        request.getToken()
                );

        UserResponse user =
                service.googleRegister(payload);

        String token = jwtUtil.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole()
        );

        return ApiResponse.success(
                Map.of(
                        "token", token,
                        "user", user
                )
        );
    }
    
    @PostMapping("/google-login")
    public ApiResponse<?> googleSignIn(
            @RequestBody @Valid GoogleTokenRequestDTO request) {

        GoogleIdToken.Payload payload =
                googleAuthService.verifyToken(
                        request.getToken()
                );

        UserResponse user =
                service.googleLogin(payload);

        String token = jwtUtil.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole()
        );

        return ApiResponse.success(
                Map.of(
                        "token", token,
                        "user", user
                )
        );
    }
}

