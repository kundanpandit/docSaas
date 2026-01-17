package com.docsaas.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.docsaas.api.dto.UserRequest;
import com.docsaas.api.dto.UserResponse;
import com.docsaas.api.model.User;
import com.docsaas.api.response.ApiResponse;
import com.docsaas.api.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    // CREATE USER
    @PostMapping
    public ApiResponse<?> create(@RequestBody @Valid UserRequest req) {

        User u = new User();
        u.setFullName(req.getFullName());
        u.setEmail(req.getEmail());
        u.setPassword(req.getPassword());
        u.setRole("USER");
        u.setStatus("ACTIVE");

        User saved = service.create(u);

        // map to response DTO (no password)
        UserResponse res = new UserResponse();
        res.setId(saved.getId());
        res.setFullName(saved.getFullName());
        res.setEmail(saved.getEmail());
        res.setRole(saved.getRole());
        res.setStatus(saved.getStatus());
        res.setCreatedAt(saved.getCreatedAt());
        res.setUpdatedAt(saved.getUpdatedAt());

        return ApiResponse.success(res);
    }

    // LIST USERS
    @GetMapping
    public ApiResponse<?> list() {
        return ApiResponse.success(service.getAllUsers());
    }
}
