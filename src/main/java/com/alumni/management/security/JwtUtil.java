package com.alumni.management.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

    @Value("${jwt.secret:ThisIsASecretKeyForJwtGeneration123456}")
    private String secret;

    // Default to 30 days for mobile application session persistence (30 * 24 * 60 * 60 * 1000)
    @Value("${jwt.expiration.ms:2592000000}")
    private long expirationMs;

    @Autowired
    private org.springframework.core.env.Environment environment;

    private Key key;

    @PostConstruct
    public void init() {
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException("JWT secret must not be empty");
        }

        boolean isProduction = false;
        for (String profile : environment.getActiveProfiles()) {
            if ("prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile)) {
                isProduction = true;
                break;
            }
        }

        // In production profile, strictly prohibit default or placeholder secrets
        if (isProduction) {
            if ("ThisIsASecretKeyForJwtGeneration123456".equals(secret) ||
                secret.contains("generate-a-strong-secret") ||
                secret.getBytes().length < 32) {
                throw new IllegalStateException(
                    "CRITICAL SECURITY CONFIGURATION ERROR: Production JWT_SECRET is missing, insecure, or shorter than 256 bits (32 bytes)."
                );
            }
        }

        byte[] secretBytes = secret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 256 bits (32 bytes).");
        }

        this.key = Keys.hmacShaKeyFor(secretBytes);
    }

    public String generateToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractEmail(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validateToken(String token) {
        try {
            extractEmail(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String refreshToken(String token) {
        try {
            String email = extractEmail(token);
            return generateToken(email);
        } catch (Exception e) {
            return null;
        }
    }
}
