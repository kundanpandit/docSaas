package com.docsaas.api.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.docsaas.api.dto.GoogleTokenRequestDTO;
import com.docsaas.api.dto.LoginRequest;
import com.docsaas.api.dto.PasswordResetDTO;
import com.docsaas.api.dto.RegisterRequestDTO;
import com.docsaas.api.dto.UserResponse;
import com.docsaas.api.response.ApiResponse;
import com.docsaas.api.security.JwtUtil;
import com.docsaas.api.service.GoogleAuthService;
import com.docsaas.api.service.PasswordResetService;
import com.docsaas.api.service.UserService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")

public class AuthController {

    private final UserService service;
    private final JwtUtil jwtUtil;
    private final GoogleAuthService googleAuthService;
    private final PasswordResetService passwordReset;

    public AuthController(
            UserService service,
            JwtUtil jwtUtil,
            GoogleAuthService googleAuthService,
            PasswordResetService passwordReset) {

        this.passwordReset = passwordReset;
        this.service = service;
        this.jwtUtil = jwtUtil;
        this.googleAuthService = googleAuthService;
    }

    /** Which optional auth features this server offers (the app hides the ones that are off). */
    @GetMapping("/options")
    public ApiResponse<?> options() {
        return ApiResponse.success(Map.of("passwordReset", passwordReset.isAvailable()));
    }

    /** Emails a reset link. Same response whether or not the address is registered. */
    @PostMapping("/forgot-password")
    public ApiResponse<?> forgotPassword(@RequestBody @Valid PasswordResetDTO.ForgotRequest request,
            HttpServletRequest http) {
        passwordReset.requestReset(request.email(), http.getRemoteAddr());
        return new ApiResponse<>(true,
                "If an account exists for that email, we've sent a link to reset the password.", null);
    }

    /** Lets the reset page check a link before the user types a new password. */
    @PostMapping("/reset-password/verify")
    public ApiResponse<?> verifyResetToken(@RequestBody @Valid PasswordResetDTO.VerifyRequest request) {
        return ApiResponse.success(Map.of("valid", passwordReset.isValid(request.token())));
    }

    @PostMapping("/reset-password")
    public ApiResponse<?> resetPassword(@RequestBody @Valid PasswordResetDTO.ResetRequest request,
            HttpServletRequest http) {
        passwordReset.resetPassword(request.token(), request.newPassword(), http.getRemoteAddr());
        return new ApiResponse<>(true, "Your password has been changed. You can log in now.", null);
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

