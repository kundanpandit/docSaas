package com.docsaas.api.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.docsaas.api.dto.AccountDTO;
import com.docsaas.api.response.ApiResponse;
import com.docsaas.api.security.SecurityUtil;
import com.docsaas.api.service.AccountService;
import com.docsaas.api.service.CreditService;

import jakarta.validation.Valid;

/** The logged-in user's own profile and credits. All endpoints require authentication. */
@RestController
@RequestMapping("/api/v1")
public class AccountController {

    private final AccountService accounts;
    private final CreditService credits;

    public AccountController(AccountService accounts, CreditService credits) {
        this.accounts = accounts;
        this.credits = credits;
    }

    @GetMapping("/users/me")
    public ApiResponse<?> me() {
        return ApiResponse.success(accounts.me());
    }

    @PatchMapping("/users/me")
    public ApiResponse<?> updateProfile(@RequestBody @Valid AccountDTO.UpdateProfile request) {
        return new ApiResponse<>(true, "Profile updated", accounts.updateProfile(request));
    }

    @PostMapping("/users/me/password")
    public ApiResponse<?> changePassword(@RequestBody @Valid AccountDTO.ChangePassword request) {
        accounts.changePassword(request);
        return new ApiResponse<>(true, "Password changed", null);
    }

    @DeleteMapping("/users/me")
    public ApiResponse<?> deleteAccount(@RequestBody @Valid AccountDTO.DeleteAccount request) {
        accounts.deleteAccount(request);
        return new ApiResponse<>(true, "Account deleted", null);
    }

    @GetMapping("/credits")
    public ApiResponse<?> credits() {
        return ApiResponse.success(credits.summary(SecurityUtil.getLoggedInUserId()));
    }
}
