package com.docsaas.api.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import com.docsaas.api.exception.ApiException;
import com.docsaas.api.model.User;
import com.docsaas.api.repository.UserRepository;

/**
 * Forgot-password flow. The emailed link carries a random 256-bit token; only its SHA-256 hash is
 * stored, so a database leak does not expose working links. Tokens are single-use, expire after
 * {@code docsaas.password-reset.valid-minutes}, and requesting a new one cancels older ones.
 *
 * The request endpoint answers the same way whether or not the email is registered, so it can't be
 * used to discover accounts.
 */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final int REQUESTS_PER_HOUR_PER_IP = 10;
    private static final int REQUESTS_PER_HOUR_PER_EMAIL = 3;
    private static final int RESETS_PER_HOUR_PER_IP = 20;

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final NamedParameterJdbcTemplate jdbc;
    private final MailService mail;
    private final RateLimiter rateLimiter;

    @Value("${docsaas.frontend-url}")
    private String frontendUrl;

    @Value("${docsaas.password-reset.valid-minutes:30}")
    private int validMinutes;

    public PasswordResetService(UserRepository users, PasswordEncoder passwordEncoder,
            NamedParameterJdbcTemplate jdbc, MailService mail, RateLimiter rateLimiter) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jdbc = jdbc;
        this.mail = mail;
        this.rateLimiter = rateLimiter;
    }

    /** Password reset needs working email. */
    public boolean isAvailable() {
        return mail.isConfigured();
    }

    /** Emails a reset link if the address belongs to an active account. Always looks the same to the caller. */
    public void requestReset(String rawEmail, String clientIp) {
        if (!isAvailable()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "EMAIL_NOT_CONFIGURED",
                    "Password reset by email isn't available yet. Please log in with Google, or contact support.");
        }
        if (!rateLimiter.tryAcquire("reset-req:ip:" + clientIp, REQUESTS_PER_HOUR_PER_IP)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED",
                    "Too many reset requests. Please try again later.");
        }
        String email = rawEmail.strip().toLowerCase(Locale.ROOT);
        // Silently capped per address so nobody can flood someone's inbox
        if (!rateLimiter.tryAcquire("reset-req:email:" + email, REQUESTS_PER_HOUR_PER_EMAIL)) {
            log.info("Password reset request ignored: hourly limit for this address reached");
            return;
        }

        User user = users.findByEmail(email).filter(u -> "ACTIVE".equalsIgnoreCase(u.getStatus())).orElse(null);
        if (user == null) {
            log.info("Password reset requested for an unknown or inactive address");
            return;
        }

        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        LocalDateTime now = LocalDateTime.now();

        // A new link replaces any earlier one
        jdbc.update("UPDATE password_reset_tokens SET used_at = :now WHERE user_id = :u AND used_at IS NULL",
                Map.of("u", user.getId(), "now", now));
        jdbc.update("""
                INSERT INTO password_reset_tokens (user_id, token_hash, expires_at, created_at)
                VALUES (:u, :h, :exp, :now)""",
                new MapSqlParameterSource().addValue("u", user.getId()).addValue("h", sha256(token))
                        .addValue("exp", now.plusMinutes(validMinutes)).addValue("now", now));

        String link = frontendUrl.replaceAll("/+$", "") + "/reset-password?token=" + token;
        mail.sendAsync(user.getEmail(), "Reset your DocSaaS password", text(user, link), html(user, link));
        log.info("Password reset link issued for user {}", user.getId());
    }

    /** Sets a new password using a link token. The token is consumed. */
    @Transactional
    public void resetPassword(String token, String newPassword, String clientIp) {
        if (!rateLimiter.tryAcquire("reset:ip:" + clientIp, RESETS_PER_HOUR_PER_IP)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED",
                    "Too many attempts. Please try again later.");
        }
        LocalDateTime now = LocalDateTime.now();
        List<Long> ids = jdbc.queryForList("""
                SELECT user_id FROM password_reset_tokens
                WHERE token_hash = :h AND used_at IS NULL AND expires_at > :now FOR UPDATE""",
                Map.of("h", sha256(token == null ? "" : token.strip()), "now", now), Long.class);
        if (ids.isEmpty()) {
            throw ApiException.badRequest("INVALID_RESET_LINK",
                    "This reset link is invalid or has expired. Please request a new one.");
        }
        User user = users.findById(ids.get(0))
                .filter(u -> "ACTIVE".equalsIgnoreCase(u.getStatus()))
                .orElseThrow(() -> ApiException.badRequest("INVALID_RESET_LINK",
                        "This reset link is invalid or has expired. Please request a new one."));

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(now);
        users.save(user);
        // Consume this link and any other outstanding ones
        jdbc.update("UPDATE password_reset_tokens SET used_at = :now WHERE user_id = :u AND used_at IS NULL",
                Map.of("u", user.getId(), "now", now));
        log.info("Password reset completed for user {}", user.getId());
    }

    /** Lets the reset page tell the user up front that a link is no longer valid. */
    public boolean isValid(String token) {
        Integer n = jdbc.queryForObject("""
                SELECT COUNT(*) FROM password_reset_tokens
                WHERE token_hash = :h AND used_at IS NULL AND expires_at > :now""",
                Map.of("h", sha256(token == null ? "" : token.strip()), "now", LocalDateTime.now()), Integer.class);
        return n != null && n > 0;
    }

    /** Housekeeping: drops tokens that expired more than a day ago. */
    public int purgeExpired() {
        return jdbc.update("DELETE FROM password_reset_tokens WHERE expires_at < :cutoff",
                Map.of("cutoff", LocalDateTime.now().minusDays(1)));
    }

    // ===== email content =====

    private String text(User user, String link) {
        return "Hi " + firstName(user) + ",\n\n"
                + "We received a request to reset the password for your DocSaaS account.\n"
                + "Open this link to choose a new password (valid for " + validMinutes + " minutes):\n\n"
                + link + "\n\n"
                + "If you didn't ask for this, you can ignore this email. Your password won't change.\n\n"
                + "- DocSaaS";
    }

    private String html(User user, String link) {
        String name = HtmlUtils.htmlEscape(firstName(user));
        String href = HtmlUtils.htmlEscape(link);
        return """
                <!doctype html><html><body style="margin:0;background:#f4f5fb;font-family:Segoe UI,Arial,sans-serif;color:#1d2033">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="padding:32px 12px">
                <tr><td align="center">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="max-width:520px;background:#ffffff;border-radius:16px;padding:32px">
                <tr><td>
                <div style="font-size:20px;font-weight:700;color:#4f46e5">DocSaaS</div>
                <div style="font-size:12px;color:#6b7086;margin-top:2px">Every document task, one place</div>
                <h1 style="font-size:20px;margin:28px 0 12px">Reset your password</h1>
                <p style="font-size:15px;line-height:1.6;margin:0 0 20px">Hi %s, we received a request to reset the password for your DocSaaS account. Click the button below to choose a new one.</p>
                <p style="margin:0 0 24px"><a href="%s" style="display:inline-block;background:#4f46e5;color:#ffffff;text-decoration:none;font-weight:600;padding:12px 22px;border-radius:10px">Choose a new password</a></p>
                <p style="font-size:13px;line-height:1.6;color:#6b7086;margin:0 0 8px">This link works once and expires in %d minutes. If the button doesn't work, copy this address into your browser:</p>
                <p style="font-size:12px;word-break:break-all;color:#4f46e5;margin:0 0 24px">%s</p>
                <p style="font-size:13px;line-height:1.6;color:#6b7086;margin:0">If you didn't ask for this, you can ignore this email. Your password won't change.</p>
                </td></tr></table>
                </td></tr></table>
                </body></html>""".formatted(name, href, validMinutes, href);
    }

    private static String firstName(User user) {
        String full = user.getFullName() == null ? "" : user.getFullName().strip();
        return full.isEmpty() ? "there" : full.split("\\s+")[0];
    }

    static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
