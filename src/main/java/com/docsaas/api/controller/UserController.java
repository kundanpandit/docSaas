package com.docsaas.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.docsaas.api.response.ApiResponse;
import com.docsaas.api.dto.UserRequest;
import com.docsaas.api.model.User;
import com.docsaas.api.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @PostMapping
    public ApiResponse<?> create(@RequestBody @Valid UserRequest req) {

        User u = new User();
        u.setFullName(req.getFullName());
        u.setEmail(req.getEmail());
        u.setPassword(req.getPassword());
        u.setRole("USER");
        u.setStatus("ACTIVE");

        return ApiResponse.success(service.create(u));
    }

    @GetMapping
    public ApiResponse<?> list() {
        return ApiResponse.success(service.getAll());
    }
}
