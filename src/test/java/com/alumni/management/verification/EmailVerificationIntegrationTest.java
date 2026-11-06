package com.alumni.management.verification;

import com.alumni.management.user.entity.User;
import com.alumni.management.user.repository.UserRepository;
import com.alumni.management.verification.dto.VerificationResponseDto;
import com.alumni.management.verification.entity.EmailVerification;
import com.alumni.management.verification.enums.VerificationPurpose;
import com.alumni.management.verification.exception.InvalidOtpException;
import com.alumni.management.verification.exception.MaxAttemptsExceededException;
import com.alumni.management.verification.exception.OtpExpiredException;
import com.alumni.management.verification.exception.ResendCooldownException;
import com.alumni.management.verification.repository.EmailVerificationRepository;
import com.alumni.management.verification.service.VerificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class EmailVerificationIntegrationTest {

    @Autowired
    private EmailVerificationRepository verificationRepository;

    @Autowired
    private VerificationService verificationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final String testEmail = "integ.verification.test@glsu.edu.in";
    private final String concurrentEmail = "concurrent.otp.test@glsu.edu.in";

    @BeforeEach
    void cleanUp() {
        cleanEmail(testEmail);
        cleanEmail(concurrentEmail);
    }

    @AfterEach
    void tearDown() {
        cleanEmail(testEmail);
        cleanEmail(concurrentEmail);
    }

    private void cleanEmail(String email) {
        List<EmailVerification> records = verificationRepository.findAllByEmailAndPurpose(email, VerificationPurpose.ACCOUNT_REGISTRATION);
        if (!records.isEmpty()) {
            verificationRepository.deleteAll(records);
        }
        List<EmailVerification> actRecords = verificationRepository.findAllByEmailAndPurpose(email, VerificationPurpose.ACCOUNT_ACTIVATION);
        if (!actRecords.isEmpty()) {
            verificationRepository.deleteAll(actRecords);
        }
        userRepository.findByEmail(email).ifPresent(userRepository::delete);
    }

    @Test
    @DisplayName("Integration Test 1: First OTP generation creates exactly one record in the database")
    void testFirstOtpGeneration_PersistsSingleRecord() {
        // Create an unverified user
        User user = new User();
        user.setEmail(testEmail);
        user.setPassword(passwordEncoder.encode("Password@123"));
        user.setName("Integ User");
        user.setVerified(false);
        userRepository.save(user);

        // Dispatches first OTP
        VerificationResponseDto response = verificationService.sendVerificationOtp(testEmail, VerificationPurpose.ACCOUNT_REGISTRATION);
        assertTrue(response.isSuccess());

        List<EmailVerification> records = verificationRepository.findAllByEmailAndPurpose(testEmail, VerificationPurpose.ACCOUNT_REGISTRATION);
        assertEquals(1, records.size(), "Database must have exactly 1 record for this email and purpose");
        EmailVerification record = records.get(0);
        assertEquals(testEmail, record.getEmail());
        assertEquals(VerificationPurpose.ACCOUNT_REGISTRATION, record.getPurpose());
        assertEquals(0, record.getAttempts());
        assertNotNull(record.getOtpHash());
        assertNull(record.getVerifiedAt());
        assertTrue(record.getExpiresAt().isAfter(LocalDateTime.now()));
    }

    @Test
    @DisplayName("Integration Test 2: Resending OTP updates the existing active record without creating a new record")
    void testResendOtp_UpdatesSameDatabaseRecordWithoutNewInsert() {
        // Pre-insert an existing active record created 70 seconds ago
        EmailVerification existing = new EmailVerification();
        existing.setEmail(testEmail);
        existing.setPurpose(VerificationPurpose.ACCOUNT_REGISTRATION);
        existing.setOtpHash(passwordEncoder.encode("000000"));
        existing.setAttempts(2);
        existing.setCreatedAt(LocalDateTime.now().minusSeconds(100));
        existing.setLastSentAt(LocalDateTime.now().minusSeconds(75)); // Cooldown elapsed
        existing.setExpiresAt(LocalDateTime.now().plusMinutes(3));
        existing = verificationRepository.saveAndFlush(existing);
        Long existingId = existing.getId();

        // Call resend
        VerificationResponseDto response = verificationService.resendOtp(testEmail, VerificationPurpose.ACCOUNT_REGISTRATION);
        assertTrue(response.isSuccess());

        List<EmailVerification> records = verificationRepository.findAllByEmailAndPurpose(testEmail, VerificationPurpose.ACCOUNT_REGISTRATION);
        assertEquals(1, records.size(), "Must not create a duplicate row in database on resend");

        EmailVerification updated = records.get(0);
        assertEquals(existingId, updated.getId(), "Must update the exact same database entity ID");
        assertEquals(0, updated.getAttempts(), "Attempts must be reset to 0 on resend");
        assertFalse(passwordEncoder.matches("000000", updated.getOtpHash()), "Old hash must be replaced with new OTP");
    }

    @Test
    @DisplayName("Integration Test 3: Verifying a valid OTP updates verifiedAt and marks user verified")
    void testVerifyOtp_ValidOtp_UpdatesDatabaseState() {
        User user = new User();
        user.setEmail(testEmail);
        user.setPassword(passwordEncoder.encode("Pass@123"));
        user.setName("Verify User");
        user.setVerified(false);
        user = userRepository.save(user);

        String rawOtp = "789123";
        EmailVerification record = new EmailVerification();
        record.setEmail(testEmail);
        record.setPurpose(VerificationPurpose.ACCOUNT_REGISTRATION);
        record.setOtpHash(passwordEncoder.encode(rawOtp));
        record.setAttempts(0);
        record.setCreatedAt(LocalDateTime.now());
        record.setLastSentAt(LocalDateTime.now());
        record.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        verificationRepository.saveAndFlush(record);

        VerificationResponseDto res = verificationService.verifyOtp(testEmail, rawOtp, VerificationPurpose.ACCOUNT_REGISTRATION);
        assertTrue(res.isSuccess());

        EmailVerification verifiedRecord = verificationRepository.findAllByEmailAndPurpose(testEmail, VerificationPurpose.ACCOUNT_REGISTRATION).get(0);
        assertNotNull(verifiedRecord.getVerifiedAt(), "verifiedAt must be stamped in PostgreSQL");

        User updatedUser = userRepository.findByEmail(testEmail).orElseThrow();
        assertTrue(updatedUser.isVerified(), "User must be marked verified in PostgreSQL");
    }

    @Test
    @DisplayName("Integration Test 4: Verifying an expired OTP throws OtpExpiredException")
    void testVerifyOtp_ExpiredOtp_ThrowsException() {
        String rawOtp = "123456";
        EmailVerification record = new EmailVerification();
        record.setEmail(testEmail);
        record.setPurpose(VerificationPurpose.ACCOUNT_REGISTRATION);
        record.setOtpHash(passwordEncoder.encode(rawOtp));
        record.setAttempts(0);
        record.setCreatedAt(LocalDateTime.now().minusMinutes(10));
        record.setLastSentAt(LocalDateTime.now().minusMinutes(10));
        record.setExpiresAt(LocalDateTime.now().minusMinutes(5)); // Expired 5 mins ago
        verificationRepository.saveAndFlush(record);

        assertThrows(OtpExpiredException.class, () ->
                verificationService.verifyOtp(testEmail, rawOtp, VerificationPurpose.ACCOUNT_REGISTRATION)
        );
    }

    @Test
    @DisplayName("Integration Test 5: Multiple verification attempts increment attempt counter until MAX_ATTEMPTS")
    void testMultipleVerificationAttempts_EnforcesAttemptLimit() {
        String correctOtp = "654321";
        EmailVerification record = new EmailVerification();
        record.setEmail(testEmail);
        record.setPurpose(VerificationPurpose.ACCOUNT_REGISTRATION);
        record.setOtpHash(passwordEncoder.encode(correctOtp));
        record.setAttempts(0);
        record.setCreatedAt(LocalDateTime.now());
        record.setLastSentAt(LocalDateTime.now());
        record.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        record = verificationRepository.saveAndFlush(record);

        // Attempt 1 with invalid OTP
        assertThrows(InvalidOtpException.class, () ->
                verificationService.verifyOtp(testEmail, "000000", VerificationPurpose.ACCOUNT_REGISTRATION)
        );
        assertEquals(1, verificationRepository.findById(record.getId()).orElseThrow().getAttempts());

        // Attempt 2
        assertThrows(InvalidOtpException.class, () ->
                verificationService.verifyOtp(testEmail, "000000", VerificationPurpose.ACCOUNT_REGISTRATION)
        );
        assertEquals(2, verificationRepository.findById(record.getId()).orElseThrow().getAttempts());

        // Manually simulate 4 failed attempts, 5th should throw MaxAttemptsExceededException
        record = verificationRepository.findById(record.getId()).orElseThrow();
        record.setAttempts(4);
        verificationRepository.saveAndFlush(record);

        assertThrows(MaxAttemptsExceededException.class, () ->
                verificationService.verifyOtp(testEmail, "000000", VerificationPurpose.ACCOUNT_REGISTRATION)
        );
        assertEquals(5, verificationRepository.findById(record.getId()).orElseThrow().getAttempts());
    }

    @Test
    @DisplayName("Integration Test 6: Duplicate records query never throws IncorrectResultSizeDataAccessException and consolidates to single active record")
    void testDuplicateRecords_RepositoryNeverThrowsIncorrectResultSizeDataAccessException() {
        // Directly inject duplicate records bypassing service to simulate legacy/historical duplicate data
        EmailVerification rec1 = new EmailVerification();
        rec1.setEmail(testEmail);
        rec1.setPurpose(VerificationPurpose.ACCOUNT_ACTIVATION);
        rec1.setOtpHash(passwordEncoder.encode("111111"));
        rec1.setAttempts(0);
        rec1.setCreatedAt(LocalDateTime.now().minusMinutes(2));
        rec1.setLastSentAt(LocalDateTime.now().minusMinutes(2));
        rec1.setExpiresAt(LocalDateTime.now().plusMinutes(3));
        rec1 = verificationRepository.saveAndFlush(rec1);

        EmailVerification rec2 = new EmailVerification();
        rec2.setEmail(testEmail);
        rec2.setPurpose(VerificationPurpose.ACCOUNT_ACTIVATION);
        rec2.setOtpHash(passwordEncoder.encode("222222"));
        rec2.setAttempts(0);
        rec2.setCreatedAt(LocalDateTime.now().minusSeconds(10));
        rec2.setLastSentAt(LocalDateTime.now().minusSeconds(10)); // Newer record
        rec2.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        rec2 = verificationRepository.saveAndFlush(rec2);

        // Calling findByEmailAndPurposeForUpdate must return List<EmailVerification> without throwing IncorrectResultSizeDataAccessException
        List<EmailVerification> found = assertDoesNotThrow(() ->
                verificationRepository.findByEmailAndPurposeForUpdate(testEmail, VerificationPurpose.ACCOUNT_ACTIVATION)
        );
        assertEquals(2, found.size(), "Both rows retrieved safely");
        assertEquals(rec2.getId(), found.get(0).getId(), "First item must be newer record due to ORDER BY v.lastSentAt DESC");

        // getActiveVerificationForUpdate must consolidate to single record and delete stale duplicate
        EmailVerification active = verificationService.getActiveVerificationForUpdate(testEmail, VerificationPurpose.ACCOUNT_ACTIVATION);
        assertNotNull(active);
        assertEquals(rec2.getId(), active.getId());

        // Verify database is cleaned up: rec1 deleted, rec2 kept
        List<EmailVerification> postConsolidation = verificationRepository.findAllByEmailAndPurpose(testEmail, VerificationPurpose.ACCOUNT_ACTIVATION);
        assertEquals(1, postConsolidation.size(), "Duplicate record must be consolidated in database");
        assertEquals(rec2.getId(), postConsolidation.get(0).getId());
    }

    @Test
    @DisplayName("Integration Test 7: Concurrent resend/send requests cannot create duplicate active OTP records")
    void testConcurrentRequests_CannotCreateDuplicateRecords() throws InterruptedException {
        int threadCount = 6;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger cooldownCount = new AtomicInteger(0);
        List<Throwable> exceptions = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Synchronize all threads to fire at the exact same millisecond
                    VerificationResponseDto res = verificationService.sendVerificationOtp(concurrentEmail, VerificationPurpose.ACCOUNT_REGISTRATION);
                    if (res.isSuccess()) {
                        successCount.incrementAndGet();
                    }
                } catch (ResendCooldownException rce) {
                    cooldownCount.incrementAndGet();
                } catch (Throwable t) {
                    exceptions.add(t);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Fire all threads concurrently
        boolean finished = doneLatch.await(10, TimeUnit.SECONDS);
        assertTrue(finished, "All concurrent requests should finish within timeout");
        executor.shutdown();

        // Print diagnostic summary
        System.out.println("Concurrent test result -> Successes: " + successCount.get()
                + ", Cooldowns: " + cooldownCount.get()
                + ", Exceptions: " + exceptions.size());

        for (Throwable t : exceptions) {
            System.err.println("Unexpected exception during concurrency: " + t.getMessage());
        }

        // Crucial assertions:
        // 1. No uncaught unexpected exceptions
        assertTrue(exceptions.isEmpty(), "Concurrent requests must not throw unexpected exceptions: " + exceptions);

        // 2. Exactly one successful request dispatched the OTP, other requests received cooldown rate limit
        assertEquals(1, successCount.get(), "Exactly one concurrent thread should win and send the OTP");
        assertEquals(threadCount - 1, cooldownCount.get(), "Losing concurrent threads must be caught by cooldown protection");

        // 3. Database MUST contain strictly 1 record for this email and purpose
        List<EmailVerification> records = verificationRepository.findAllByEmailAndPurpose(concurrentEmail, VerificationPurpose.ACCOUNT_REGISTRATION);
        assertEquals(1, records.size(), "Database must strictly contain exactly 1 record after concurrent requests");
    }
}
