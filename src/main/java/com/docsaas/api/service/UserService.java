package com.docsaas.api.service;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.docsaas.api.dto.UserResponse;
import com.docsaas.api.model.User;
import com.docsaas.api.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository repo;
    private final PasswordEncoder passwordEncoder;
    public UserService(UserRepository repo, PasswordEncoder passwordEncoder) {
        this.repo = repo;
        this.passwordEncoder = passwordEncoder;
    }

    public User create(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return repo.save(user);
    }

    public Iterable<User> getAll() {
        return repo.findAll();
    }
    
    public UserResponse login(String email, String rawPassword) {

        User user = repo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        UserResponse res = new UserResponse();
        res.setId(user.getId());
        res.setFullName(user.getFullName());
        res.setEmail(user.getEmail());
        res.setRole(user.getRole());
        res.setStatus(user.getStatus());
        res.setCreatedAt(user.getCreatedAt());
        res.setUpdatedAt(user.getUpdatedAt());

        return res;
    }


}
