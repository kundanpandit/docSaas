package com.docsaas.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Requests for the logged-in user's own account (FR-026). */
public final class AccountDTO {

    private AccountDTO() {
    }

    public record UpdateProfile(
            @NotBlank(message = "Full name is required")
            @Size(max = 150, message = "Full name must be at most 150 characters")
            String fullName) {
    }

    public record ChangePassword(
            @NotBlank(message = "Current password is required")
            String currentPassword,
            @NotBlank(message = "New password is required")
            @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
            String newPassword) {
    }

    /** The user types their email address to confirm, which also works for Google-only accounts. */
    public record DeleteAccount(
            @NotBlank(message = "Please type your email address to confirm")
            String confirmEmail) {
    }
}
