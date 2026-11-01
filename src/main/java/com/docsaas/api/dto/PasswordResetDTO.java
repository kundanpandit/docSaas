package com.docsaas.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class PasswordResetDTO {

    private PasswordResetDTO() {
    }

    public record ForgotRequest(
            @NotBlank(message = "Email is required")
            @Email(message = "Enter a valid email address")
            @Size(max = 150, message = "Email must be at most 150 characters")
            String email) {
    }

    public record ResetRequest(
            @NotBlank(message = "The reset link is missing its token")
            @Size(max = 200, message = "The reset link is not valid")
            String token,
            @NotBlank(message = "New password is required")
            @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
            String newPassword) {
    }

    public record VerifyRequest(
            @NotBlank(message = "The reset link is missing its token")
            @Size(max = 200, message = "The reset link is not valid")
            String token) {
    }
}
