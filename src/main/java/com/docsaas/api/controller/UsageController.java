package com.docsaas.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.docsaas.api.response.ApiResponse;
import com.docsaas.api.security.SecurityUtil;
import com.docsaas.api.service.CreditService;

import jakarta.servlet.http.HttpServletRequest;

/** Today's credits and usage for whoever is calling: a guest (by IP) or the logged-in user. Public. */
@RestController
@RequestMapping("/api/v1/usage")
public class UsageController {

    private final CreditService credits;

    public UsageController(CreditService credits) {
        this.credits = credits;
    }

    @GetMapping
    public ApiResponse<?> usage(HttpServletRequest request) {
        return ApiResponse.success(credits.usage(SecurityUtil.getUserIdOrNull(), request.getRemoteAddr()));
    }
}
