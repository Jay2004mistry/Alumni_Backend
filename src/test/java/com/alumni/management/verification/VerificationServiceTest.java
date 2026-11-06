package com.alumni.management.verification;

import com.alumni.management.email.service.EmailService;
import com.alumni.management.user.entity.User;
import com.alumni.management.user.repository.UserRepository;
import com.alumni.management.verification.dto.VerificationResponseDto;
import com.alumni.management.verification.entity.EmailVerification;
import com.alumni.management.verification.enums.VerificationPurpose;
import com.alumni.management.verification.exception.*;
import com.alumni.management.verification.repository.EmailVerificationRepository;
import com.alumni.management.verification.service.VerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificationServiceTest {

    @Mock
    private EmailVerificationRepository verificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    private PasswordEncoder passwordEncoder;
    private VerificationService verificationService;

    private final String testEmail = "test@gls.edu.in";

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        verificationService = new VerificationService(
                verificationRepository,
                userRepository,
                emailService,
                passwordEncoder
        );
    }

    @Test
    @DisplayName("OTP generator must produce exactly 6 digits with leading zeros preserved")
    void testOtpGeneration_LengthAndDigits() {
        for (int i = 0; i < 1000; i++) {
            String otp = verificationService.generateSecureSixDigitOtp();
            assertNotNull(otp);
            assertEquals(6, otp.length(), "OTP must be exactly 6 digits");
            assertTrue(otp.matches("^[0-9]{6}$"), "OTP must contain only digits: " + otp);
        }
    }

    @Test
    @DisplayName("1. First OTP generation creates new record, stores hashed OTP, and dispatches email")
    void testFirstOtpGeneration_CreatesNewRecordAndDispatchesOtp() {
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.empty());
        when(verificationRepository.findByEmailAndPurposeForUpdate(testEmail, VerificationPurpose.ACCOUNT_REGISTRATION))
                .thenReturn(Collections.emptyList());
        when(verificationRepository.findByEmailAndPurposeForUpdate(testEmail, VerificationPurpose.ACCOUNT_ACTIVATION))
                .thenReturn(Collections.emptyList());

        VerificationResponseDto response = verificationService.sendVerificationOtp(
                testEmail,
                VerificationPurpose.ACCOUNT_REGISTRATION
        );

        assertTrue(response.isSuccess());
        assertEquals(60, response.getCooldownSeconds());

        ArgumentCaptor<EmailVerification> captor = ArgumentCaptor.forClass(EmailVerification.class);
        verify(verificationRepository).saveAndFlush(captor.capture());

        EmailVerification saved = captor.getValue();
        assertEquals(testEmail, saved.getEmail());
        assertEquals(VerificationPurpose.ACCOUNT_REGISTRATION, saved.getPurpose());
        assertEquals(0, saved.getAttempts());
        assertNotNull(saved.getOtpHash());
        assertNotEquals(6, saved.getOtpHash().length(), "Stored hash must NOT be a plaintext 6-digit OTP");
        assertTrue(saved.getOtpHash().startsWith("$2a$") || saved.getOtpHash().startsWith("$2b$"), "Must be a BCrypt hash");

        verify(emailService, times(1)).sendVerificationOtpEmail(eq(testEmail), anyString(), eq(VerificationPurpose.ACCOUNT_REGISTRATION));
    }

    @Test
    @DisplayName("Send OTP rejects already verified users during registration")
    void testSendVerificationOtp_AlreadyVerified_ThrowsUserAlreadyExistsException() {
        User verifiedUser = new User();
        verifiedUser.setEmail(testEmail);
        verifiedUser.setVerified(true);

        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(verifiedUser));

        assertThrows(UserAlreadyExistsException.class, () ->
                verificationService.sendVerificationOtp(testEmail, VerificationPurpose.ACCOUNT_REGISTRATION)
        );

        verify(emailService, never()).sendVerificationOtpEmail(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Send OTP enforces 60-second cooldown")
    void testSendVerificationOtp_EnforcesCooldown() {
        EmailVerification existing = new EmailVerification();
        existing.setEmail(testEmail);
        existing.setPurpose(VerificationPurpose.ACCOUNT_REGISTRATION);
        existing.setLastSentAt(LocalDateTime.now().minusSeconds(25)); // Sent 25s ago (cooldown is 60s)

        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.empty());
        when(verificationRepository.findByEmailAndPurposeForUpdate(testEmail, VerificationPurpose.ACCOUNT_REGISTRATION))
                .thenReturn(List.of(existing));

        ResendCooldownException ex = assertThrows(ResendCooldownException.class, () ->
                verificationService.sendVerificationOtp(testEmail, VerificationPurpose.ACCOUNT_REGISTRATION)
        );

        assertTrue(ex.getSecondsRemaining() > 0 && ex.getSecondsRemaining() <= 35);
        verify(emailService, never()).sendVerificationOtpEmail(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Email delivery failure resets cooldown to allow immediate retry")
    void testSendVerificationOtp_EmailDeliveryFailure_ResetsCooldown() {
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.empty());
        when(verificationRepository.findByEmailAndPurposeForUpdate(testEmail, VerificationPurpose.ACCOUNT_REGISTRATION))
                .thenReturn(Collections.emptyList());
        when(verificationRepository.findByEmailAndPurposeForUpdate(testEmail, VerificationPurpose.ACCOUNT_ACTIVATION))
                .thenReturn(Collections.emptyList());

        doThrow(new EmailDeliveryException("SMTP timeout"))
                .when(emailService).sendVerificationOtpEmail(eq(testEmail), anyString(), any());

        assertThrows(EmailDeliveryException.class, () ->
                verificationService.sendVerificationOtp(testEmail, VerificationPurpose.ACCOUNT_REGISTRATION)
        );

        // Verification repository should have been updated to reset lastSentAt
        verify(verificationRepository, times(1)).save(any(EmailVerification.class));
    }

    @Test
    @DisplayName("2. Resending OTP updates existing active record rather than inserting new record")
    void testResendingOtp_UpdatesExistingActiveRecord_NoNewInsert() {
        EmailVerification existing = new EmailVerification();
        existing.setId(101L);
        existing.setEmail(testEmail);
        existing.setPurpose(VerificationPurpose.ACCOUNT_REGISTRATION);
        existing.setOtpHash(passwordEncoder.encode("111111"));
        existing.setAttempts(3);
        existing.setLastSentAt(LocalDateTime.now().minusSeconds(75)); // Cooldown elapsed

        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.empty());
        when(verificationRepository.findByEmailAndPurposeForUpdate(testEmail, VerificationPurpose.ACCOUNT_REGISTRATION))
                .thenReturn(List.of(existing));

        VerificationResponseDto response = verificationService.resendOtp(
                testEmail,
                VerificationPurpose.ACCOUNT_REGISTRATION
        );

        assertTrue(response.isSuccess());
        assertEquals("A new verification code has been sent to your email.", response.getMessage());

        // Verify the existing instance is updated and flushed
        ArgumentCaptor<EmailVerification> captor = ArgumentCaptor.forClass(EmailVerification.class);
        verify(verificationRepository).saveAndFlush(captor.capture());

        EmailVerification saved = captor.getValue();
        assertEquals(101L, saved.getId(), "Must preserve existing entity ID");
        assertEquals(0, saved.getAttempts(), "Attempts counter must be reset on resend");
        assertFalse(passwordEncoder.matches("111111", saved.getOtpHash()), "Old OTP must be replaced by new hash");
        assertNull(saved.getVerifiedAt());
        verify(emailService, times(1)).sendVerificationOtpEmail(eq(testEmail), anyString(), eq(VerificationPurpose.ACCOUNT_REGISTRATION));
    }

    @Test
    @DisplayName("3. Verifying a valid OTP marks record as verified and activates user")
    void testVerifyingValidOtp_MarksVerified_ActivatesUser() {
        String plaintextOtp = "123456";
        EmailVerification verification = new EmailVerification();
        verification.setId(202L);
        verification.setEmail(testEmail);
        verification.setPurpose(VerificationPurpose.ACCOUNT_REGISTRATION);
        verification.setOtpHash(passwordEncoder.encode(plaintextOtp));
        verification.setAttempts(0);
        verification.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        verification.setVerifiedAt(null);

        User pendingUser = new User();
        pendingUser.setEmail(testEmail);
        pendingUser.setVerified(false);

        when(verificationRepository.findByEmailAndPurposeForUpdate(testEmail, VerificationPurpose.ACCOUNT_REGISTRATION))
                .thenReturn(List.of(verification));
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(pendingUser));

        VerificationResponseDto response = verificationService.verifyOtp(
                testEmail,
                plaintextOtp,
                VerificationPurpose.ACCOUNT_REGISTRATION
        );

        assertTrue(response.isSuccess());
        assertNotNull(verification.getVerifiedAt(), "verifiedAt must be stamped");
        assertTrue(pendingUser.isVerified(), "User account must be marked verified");

        verify(verificationRepository).save(verification);
        verify(userRepository).save(pendingUser);
    }

    @Test
    @DisplayName("OTP created under ACCOUNT_ACTIVATION seamlessly verifies when submitted with ACCOUNT_REGISTRATION")
    void testVerifyOtp_CrossPurposeAccountVerification_Succeeds() {
        String plaintextOtp = "654321";
        EmailVerification verification = new EmailVerification();
        verification.setEmail(testEmail);
        verification.setPurpose(VerificationPurpose.ACCOUNT_ACTIVATION);
        verification.setOtpHash(passwordEncoder.encode(plaintextOtp));
        verification.setAttempts(0);
        verification.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        verification.setVerifiedAt(null);

        User pendingUser = new User();
        pendingUser.setEmail(testEmail);
        pendingUser.setVerified(false);

        // Direct query for ACCOUNT_REGISTRATION returns empty
        when(verificationRepository.findByEmailAndPurposeForUpdate(testEmail, VerificationPurpose.ACCOUNT_REGISTRATION))
                .thenReturn(Collections.emptyList());
        // Fallback queries ACCOUNT_ACTIVATION and finds it
        when(verificationRepository.findByEmailAndPurposeForUpdate(testEmail, VerificationPurpose.ACCOUNT_ACTIVATION))
                .thenReturn(List.of(verification));
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(pendingUser));

        VerificationResponseDto response = verificationService.verifyOtp(
                testEmail,
                plaintextOtp,
                VerificationPurpose.ACCOUNT_REGISTRATION
        );

        assertTrue(response.isSuccess());
        assertNotNull(verification.getVerifiedAt());
        assertTrue(pendingUser.isVerified());
        verify(verificationRepository).save(verification);
        verify(userRepository).save(pendingUser);
    }

    @Test
    @DisplayName("4. Verifying an expired OTP throws OtpExpiredException")
    void testVerifyingExpiredOtp_ThrowsOtpExpiredException() {
        EmailVerification verification = new EmailVerification();
        verification.setEmail(testEmail);
        verification.setPurpose(VerificationPurpose.ACCOUNT_REGISTRATION);
        verification.setOtpHash(passwordEncoder.encode("123456"));
        verification.setAttempts(0);
        verification.setExpiresAt(LocalDateTime.now().minusSeconds(10)); // Expired 10s ago

        when(verificationRepository.findByEmailAndPurposeForUpdate(testEmail, VerificationPurpose.ACCOUNT_REGISTRATION))
                .thenReturn(List.of(verification));

        assertThrows(OtpExpiredException.class, () ->
                verificationService.verifyOtp(testEmail, "123456", VerificationPurpose.ACCOUNT_REGISTRATION)
        );
    }

    @Test
    @DisplayName("5. Multiple verification attempts increment attempt counter until MAX_ATTEMPTS reached")
    void testMultipleOtpVerificationAttempts_IncrementsAttemptsUntilMax() {
        String correctOtp = "123456";
        String incorrectOtp = "654321";

        EmailVerification verification = new EmailVerification();
        verification.setEmail(testEmail);
        verification.setPurpose(VerificationPurpose.ACCOUNT_REGISTRATION);
        verification.setOtpHash(passwordEncoder.encode(correctOtp));
        verification.setAttempts(1);
        verification.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        verification.setVerifiedAt(null);

        when(verificationRepository.findByEmailAndPurposeForUpdate(testEmail, VerificationPurpose.ACCOUNT_REGISTRATION))
                .thenReturn(List.of(verification));

        InvalidOtpException ex = assertThrows(InvalidOtpException.class, () ->
                verificationService.verifyOtp(testEmail, incorrectOtp, VerificationPurpose.ACCOUNT_REGISTRATION)
        );

        assertTrue(ex.getMessage().contains("3 attempts remaining"));
        assertEquals(2, verification.getAttempts());
        verify(verificationRepository).saveAndFlush(verification);

        // Now test 5th failed attempt triggers MaxAttemptsExceededException
        verification.setAttempts(4);
        assertThrows(MaxAttemptsExceededException.class, () ->
                verificationService.verifyOtp(testEmail, incorrectOtp, VerificationPurpose.ACCOUNT_REGISTRATION)
        );
        assertEquals(5, verification.getAttempts());
    }

    @Test
    @DisplayName("Already verified/consumed OTP cannot be reused")
    void testVerifyOtp_AlreadyConsumed() {
        EmailVerification verification = new EmailVerification();
        verification.setEmail(testEmail);
        verification.setPurpose(VerificationPurpose.ACCOUNT_REGISTRATION);
        verification.setOtpHash(passwordEncoder.encode("123456"));
        verification.setVerifiedAt(LocalDateTime.now().minusMinutes(1)); // already verified

        when(verificationRepository.findByEmailAndPurposeForUpdate(testEmail, VerificationPurpose.ACCOUNT_REGISTRATION))
                .thenReturn(List.of(verification));

        InvalidOtpException ex = assertThrows(InvalidOtpException.class, () ->
                verificationService.verifyOtp(testEmail, "123456", VerificationPurpose.ACCOUNT_REGISTRATION)
        );

        assertTrue(ex.getMessage().contains("already been used"));
    }

    @Test
    @DisplayName("6. Duplicate records handling selects active record and automatically cleans up stale duplicates without IncorrectResultSizeDataAccessException")
    void testDuplicateRecords_ConsolidatesToLatestAndCleansUpStaleDuplicates() {
        // Simulate historical duplicate records in database for the same email and purpose
        EmailVerification latestActive = new EmailVerification();
        latestActive.setId(35L);
        latestActive.setEmail(testEmail);
        latestActive.setPurpose(VerificationPurpose.ACCOUNT_REGISTRATION);
        latestActive.setOtpHash(passwordEncoder.encode("888888"));
        latestActive.setLastSentAt(LocalDateTime.now().minusSeconds(20));
        latestActive.setExpiresAt(LocalDateTime.now().plusMinutes(5));

        EmailVerification staleDuplicate = new EmailVerification();
        staleDuplicate.setId(34L);
        staleDuplicate.setEmail(testEmail);
        staleDuplicate.setPurpose(VerificationPurpose.ACCOUNT_REGISTRATION);
        staleDuplicate.setOtpHash(passwordEncoder.encode("999999"));
        staleDuplicate.setLastSentAt(LocalDateTime.now().minusMinutes(10));
        staleDuplicate.setExpiresAt(LocalDateTime.now().minusMinutes(5));

        // When repository returns multiple records ordered by lastSentAt DESC
        when(verificationRepository.findByEmailAndPurposeForUpdate(testEmail, VerificationPurpose.ACCOUNT_REGISTRATION))
                .thenReturn(List.of(latestActive, staleDuplicate));

        EmailVerification retrieved = verificationService.getActiveVerificationForUpdate(testEmail, VerificationPurpose.ACCOUNT_REGISTRATION);

        assertNotNull(retrieved);
        assertEquals(35L, retrieved.getId(), "Must select latest active record");

        // Verify stale duplicate (id: 34) was deleted and flushed
        verify(verificationRepository).delete(staleDuplicate);
        verify(verificationRepository).flush();
    }

    @Test
    @DisplayName("7. Concurrent resend requests hitting unique constraint throw ResendCooldownException instead of crashing")
    void testConcurrentResendRequests_DataIntegrityViolation_EnforcesCooldown() {
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.empty());
        when(verificationRepository.findByEmailAndPurposeForUpdate(testEmail, VerificationPurpose.ACCOUNT_REGISTRATION))
                .thenReturn(Collections.emptyList());
        when(verificationRepository.findByEmailAndPurposeForUpdate(testEmail, VerificationPurpose.ACCOUNT_ACTIVATION))
                .thenReturn(Collections.emptyList());

        // Simulate concurrent insert unique index violation
        when(verificationRepository.saveAndFlush(any(EmailVerification.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint uk_email_verification_email_purpose"));

        ResendCooldownException ex = assertThrows(ResendCooldownException.class, () ->
                verificationService.sendVerificationOtp(testEmail, VerificationPurpose.ACCOUNT_REGISTRATION)
        );

        assertTrue(ex.getMessage().contains("A verification code was already requested"));
        assertEquals(60, ex.getSecondsRemaining());
        verify(emailService, never()).sendVerificationOtpEmail(anyString(), anyString(), any());
    }
}
