package com.docsaas.api.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.docsaas.api.dto.RegisterRequestDTO;
import com.docsaas.api.dto.UserListResponse;
import com.docsaas.api.dto.UserResponse;
import com.docsaas.api.exception.ApiException;
import com.docsaas.api.model.User;
import com.docsaas.api.repository.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;

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
                .filter(u -> passwordEncoder.matches(rawPassword, u.getPassword()))
                // Same message for unknown email and wrong password, to avoid account enumeration
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password"));

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED", "This account is disabled");
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
            .orElseThrow(() -> ApiException.notFound("User not found"));

        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new ApiException(HttpStatus.CONFLICT, "ALREADY_ADMIN", "User is already an ADMIN");
        }

        user.setRole("ADMIN");
        user.setUpdatedAt(LocalDateTime.now());

        repo.save(user);
    }
    
    public UserResponse register(RegisterRequestDTO request) {

        if (repo.findByEmail(request.getEmail()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_EXISTS", "Email already registered");
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        user.setRole("USER");
        user.setStatus("ACTIVE");

        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        User savedUser = repo.save(user);

        UserResponse response = new UserResponse();
        response.setId(savedUser.getId());
        response.setFullName(savedUser.getFullName());
        response.setEmail(savedUser.getEmail());
        response.setRole(savedUser.getRole());
        response.setStatus(savedUser.getStatus());
        response.setCreatedAt(savedUser.getCreatedAt());
        response.setUpdatedAt(savedUser.getUpdatedAt());

        return response;
    }
    
    public UserResponse googleRegister(
            GoogleIdToken.Payload payload) {

        String email = payload.getEmail();

        if (repo.findByEmail(email).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_EXISTS", "Account already exists. Please login.");
        }

        User user = new User();

        user.setFullName(
                (String) payload.get("name")
        );

        user.setEmail(email);

        user.setPassword(
                passwordEncoder.encode(
                        UUID.randomUUID().toString()
                )
        );

        user.setRole("USER");
        user.setStatus("ACTIVE");

        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        user = repo.save(user);

        UserResponse response = new UserResponse();

        response.setId(user.getId());
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        response.setStatus(user.getStatus());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());

        return response;
    }
    
    public UserResponse googleLogin(
            GoogleIdToken.Payload payload) {

        String email = payload.getEmail();

        User user = repo.findByEmail(email)
                .orElseThrow(() ->
                        new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_REGISTERED", "User not registered. Please create an account first."));

        UserResponse response = new UserResponse();

        response.setId(user.getId());
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        response.setStatus(user.getStatus());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());

        return response;
    }


}
