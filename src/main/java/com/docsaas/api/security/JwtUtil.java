package com.docsaas.api.security;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    // 🔐 Signing key
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    // 🔹 Generate JWT
    public String generateToken(Long userId, String email, String role) {
        return Jwts.builder()
                .setSubject(email)            // still okay to keep email as subject
                .claim("userId", userId)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // Extract all claims
    public Claims extractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // Validate token
    public boolean isTokenValid(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    //  Extract userId (used in JwtFilter)
    public Long extractUserId(String token) {
        return extractClaims(token).get("userId", Long.class);
    }

    // Extract role (optional helper)
    public String extractRole(String token) {
        return extractClaims(token).get("role", String.class);
    }

    // Extract email (optional, not required for filter)
    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }
}
