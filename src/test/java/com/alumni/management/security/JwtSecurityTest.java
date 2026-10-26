package com.alumni.management.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

public class JwtSecurityTest {

    private static final String STRONG_SECRET = "ThisIsAStrongCryptographicallySecureSecretKey1234567890!";

    @Test
    @DisplayName("JwtUtil correctly generates and validates token with strong secret")
    void testGenerateAndValidateToken() {
        JwtUtil jwtUtil = new JwtUtil();
        MockEnvironment env = new MockEnvironment();
        ReflectionTestUtils.setField(jwtUtil, "environment", env);
        ReflectionTestUtils.setField(jwtUtil, "secret", STRONG_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", 3600000L); // 1 hour
        jwtUtil.init();

        String token = jwtUtil.generateToken("user@example.com");
        assertNotNull(token);
        assertTrue(jwtUtil.validateToken(token));
        assertEquals("user@example.com", jwtUtil.extractEmail(token));
    }

    @Test
    @DisplayName("JwtUtil rejects token with expired timestamp")
    void testExpiredTokenValidation() throws InterruptedException {
        JwtUtil jwtUtil = new JwtUtil();
        MockEnvironment env = new MockEnvironment();
        ReflectionTestUtils.setField(jwtUtil, "environment", env);
        ReflectionTestUtils.setField(jwtUtil, "secret", STRONG_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", 1L); // 1 millisecond
        jwtUtil.init();

        String token = jwtUtil.generateToken("user@example.com");
        Thread.sleep(10); // Wait for expiration

        assertFalse(jwtUtil.validateToken(token));
    }

    @Test
    @DisplayName("JwtUtil rejects tampered or invalid token signature")
    void testTamperedTokenValidation() {
        JwtUtil jwtUtil = new JwtUtil();
        MockEnvironment env = new MockEnvironment();
        ReflectionTestUtils.setField(jwtUtil, "environment", env);
        ReflectionTestUtils.setField(jwtUtil, "secret", STRONG_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", 3600000L);
        jwtUtil.init();

        String token = jwtUtil.generateToken("user@example.com");
        String tamperedToken = token.substring(0, token.length() - 5) + "ABCDE";

        assertFalse(jwtUtil.validateToken(tamperedToken));
    }

    @Test
    @DisplayName("JwtUtil fails startup in production profile if secret uses default value")
    void testProductionFailsWithDefaultSecret() {
        JwtUtil jwtUtil = new JwtUtil();
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        ReflectionTestUtils.setField(jwtUtil, "environment", env);
        ReflectionTestUtils.setField(jwtUtil, "secret", "ThisIsASecretKeyForJwtGeneration123456");
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", 3600000L);

        IllegalStateException ex = assertThrows(IllegalStateException.class, jwtUtil::init);
        assertTrue(ex.getMessage().contains("CRITICAL SECURITY CONFIGURATION ERROR"));
    }

    @Test
    @DisplayName("JwtUtil rejects secret with fewer than 256 bits (32 bytes)")
    void testShortSecretRejected() {
        JwtUtil jwtUtil = new JwtUtil();
        MockEnvironment env = new MockEnvironment();
        ReflectionTestUtils.setField(jwtUtil, "environment", env);
        ReflectionTestUtils.setField(jwtUtil, "secret", "short-secret-key-16b");
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", 3600000L);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, jwtUtil::init);
        assertTrue(ex.getMessage().contains("at least 256 bits"));
    }
}
