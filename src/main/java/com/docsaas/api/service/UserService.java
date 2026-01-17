package com.docsaas.api.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.StreamSupport;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.docsaas.api.dto.UserListResponse;
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

    public List<UserListResponse> getAllUsers() {

        return StreamSupport.stream(repo.findAll().spliterator(), false)
                .map(user -> {

                    UserListResponse res = new UserListResponse();
                    res.setId(user.getId());
                    res.setFullName(user.getFullName());
                    res.setEmail(user.getEmail());
                    res.setRole(user.getRole());
                    res.setStatus(user.getStatus());
                    res.setCreatedAt(user.getCreatedAt());
                    res.setUpdatedAt(user.getUpdatedAt());

                    return res;

                })
                .toList();
    }
    
    
    public void promoteToAdmin(Long userId) {
        User user = repo.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        user.setRole("ADMIN");
        user.setUpdatedAt(LocalDateTime.now());

        repo.save(user);
    }

}
