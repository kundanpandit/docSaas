package com.docsaas.api.service;

import java.time.LocalDateTime;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.docsaas.api.dto.AccountDTO;
import com.docsaas.api.dto.UserResponse;
import com.docsaas.api.exception.ApiException;
import com.docsaas.api.model.Job;
import com.docsaas.api.model.User;
import com.docsaas.api.repository.JobRepository;
import com.docsaas.api.repository.UserRepository;
import com.docsaas.api.security.SecurityUtil;

/** Profile, password and account deletion for the logged-in user (FR-026). */
@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final UserRepository users;
    private final JobRepository jobs;
    private final StorageService storage;
    private final CreditService credits;
    private final PasswordEncoder passwordEncoder;
    private final NamedParameterJdbcTemplate jdbc;

    public AccountService(UserRepository users, JobRepository jobs, StorageService storage, CreditService credits,
            PasswordEncoder passwordEncoder, NamedParameterJdbcTemplate jdbc) {
        this.users = users;
        this.jobs = jobs;
        this.storage = storage;
        this.credits = credits;
        this.passwordEncoder = passwordEncoder;
        this.jdbc = jdbc;
    }

    public UserResponse me() {
        return toResponse(current());
    }

    public UserResponse updateProfile(AccountDTO.UpdateProfile request) {
        User user = current();
        user.setFullName(request.fullName().strip());
        user.setUpdatedAt(LocalDateTime.now());
        return toResponse(users.save(user));
    }

    public void changePassword(AccountDTO.ChangePassword request) {
        User user = current();
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw ApiException.badRequest("WRONG_PASSWORD", "Your current password is not correct");
        }
        if (request.currentPassword().equals(request.newPassword())) {
            throw ApiException.badRequest("SAME_PASSWORD", "The new password must be different from the current one");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        users.save(user);
        log.info("Password changed for user {}", user.getId());
    }

    /** Deletes the account with its jobs, stored files and credit history. Not reversible. */
    @Transactional
    public void deleteAccount(AccountDTO.DeleteAccount request) {
        User user = current();
        if (!user.getEmail().equalsIgnoreCase(request.confirmEmail().strip())) {
            throw ApiException.badRequest("CONFIRMATION_MISMATCH", "The email address does not match your account");
        }
        Long userId = user.getId();
        for (Job job : jobs.findRecentByUserId(userId, Integer.MAX_VALUE)) {
            storage.deleteTreeQuietly(JobService.jobDir(job));
        }
        Map<String, Long> p = Map.of("u", userId);
        jdbc.update("DELETE FROM job_files WHERE job_id IN (SELECT id FROM jobs WHERE user_id = :u)", p);
        jdbc.update("DELETE FROM jobs WHERE user_id = :u", p);
        jdbc.update("DELETE FROM documents WHERE user_id = :u", p);
        credits.deleteAll(userId);
        users.delete(user);
        log.info("Account {} deleted at the user's request", userId);
    }

    private User current() {
        return users.findById(SecurityUtil.getLoggedInUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Please log in to continue"));
    }

    private static UserResponse toResponse(User user) {
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
