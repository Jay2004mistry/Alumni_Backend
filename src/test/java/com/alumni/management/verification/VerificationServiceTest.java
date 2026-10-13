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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
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

    private final String testEmail = "test@example.com";

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
    @DisplayName("Send OTP stores secure hash, never plaintext, and dispatches email")
    void testSendVerificationOtp_StoresHashedOtp() {
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.empty());
        when(verificationRepository.findByEmailAndPurposeForUpdate(testEmail, VerificationPurpose.ACCOUNT_REGISTRATION))
                .thenReturn(Optional.empty());

        VerificationResponseDto response = verificationService.sendVerificationOtp(
                testEmail,
                VerificationPurpose.ACCOUNT_REGISTRATION
        );

        assertTrue(response.isSuccess());
        assertEquals(60, response.getCooldownSeconds());

        ArgumentCaptor<EmailVerification> captor = ArgumentCaptor.forClass(EmailVerification.class);
        verify(verificationRepository).save(captor.capture());

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
                .thenReturn(Optional.of(existing));

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
                .thenReturn(Optional.empty());

        doThrow(new EmailDeliveryException("SMTP timeout"))
                .when(emailService).sendVerificationOtpEmail(eq(testEmail), anyString(), any());

        assertThrows(EmailDeliveryException.class, () ->
                verificationService.sendVerificationOtp(testEmail, VerificationPurpose.ACCOUNT_REGISTRATION)
        );

        // Verification repository should have been updated to reset lastSentAt
        verify(verificationRepository, times(2)).save(any(EmailVerification.class));
    }

    @Test
    @DisplayName("Successful OTP verification marks record and updates user verified status")
    void testVerifyOtp_Success() {
        String plaintextOtp = "123456";
        EmailVerification verification = new EmailVerification();
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
                .thenReturn(Optional.of(verification));
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
    @DisplayName("Incorrect OTP increments attempt counter and throws InvalidOtpException")
    void testVerifyOtp_IncorrectOtp_IncrementsAttempts() {
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
                .thenReturn(Optional.of(verification));

        InvalidOtpException ex = assertThrows(InvalidOtpException.class, () ->
                verificationService.verifyOtp(testEmail, incorrectOtp, VerificationPurpose.ACCOUNT_REGISTRATION)
        );

        assertTrue(ex.getMessage().contains("3 attempts remaining"));
        assertEquals(2, verification.getAttempts());
        verify(verificationRepository).save(verification);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Fifth failed verification attempt triggers MaxAttemptsExceededException")
    void testVerifyOtp_MaxAttemptsExceeded() {
        String correctOtp = "123456";
        String incorrectOtp = "000000";

        EmailVerification verification = new EmailVerification();
        verification.setEmail(testEmail);
        verification.setPurpose(VerificationPurpose.ACCOUNT_REGISTRATION);
        verification.setOtpHash(passwordEncoder.encode(correctOtp));
        verification.setAttempts(4); // 4 prior failed attempts
        verification.setExpiresAt(LocalDateTime.now().plusMinutes(5));

        when(verificationRepository.findByEmailAndPurposeForUpdate(testEmail, VerificationPurpose.ACCOUNT_REGISTRATION))
                .thenReturn(Optional.of(verification));

        assertThrows(MaxAttemptsExceededException.class, () ->
                verificationService.verifyOtp(testEmail, incorrectOtp, VerificationPurpose.ACCOUNT_REGISTRATION)
        );

        assertEquals(5, verification.getAttempts());
        verify(verificationRepository).save(verification);
    }

    @Test
    @DisplayName("Expired OTP is rejected with OtpExpiredException")
    void testVerifyOtp_Expired() {
        EmailVerification verification = new EmailVerification();
        verification.setEmail(testEmail);
        verification.setPurpose(VerificationPurpose.ACCOUNT_REGISTRATION);
        verification.setOtpHash(passwordEncoder.encode("123456"));
        verification.setAttempts(0);
        verification.setExpiresAt(LocalDateTime.now().minusSeconds(10)); // expired 10s ago

        when(verificationRepository.findByEmailAndPurposeForUpdate(testEmail, VerificationPurpose.ACCOUNT_REGISTRATION))
                .thenReturn(Optional.of(verification));

        assertThrows(OtpExpiredException.class, () ->
                verificationService.verifyOtp(testEmail, "123456", VerificationPurpose.ACCOUNT_REGISTRATION)
        );
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
                .thenReturn(Optional.of(verification));

        InvalidOtpException ex = assertThrows(InvalidOtpException.class, () ->
                verificationService.verifyOtp(testEmail, "123456", VerificationPurpose.ACCOUNT_REGISTRATION)
        );

        assertTrue(ex.getMessage().contains("already been used"));
    }

    @Test
    @DisplayName("Cross-purpose OTP validation is rejected")
    void testVerifyOtp_CrossPurposeRejected() {
        // Verification was generated for ACCOUNT_ACTIVATION, but verify is called with ACCOUNT_REGISTRATION
        when(verificationRepository.findByEmailAndPurposeForUpdate(testEmail, VerificationPurpose.ACCOUNT_REGISTRATION))
                .thenReturn(Optional.empty());

        assertThrows(InvalidOtpException.class, () ->
                verificationService.verifyOtp(testEmail, "123456", VerificationPurpose.ACCOUNT_REGISTRATION)
        );
    }

    @Test
    @DisplayName("Resending OTP supersedes previous active OTP and resets attempts")
    void testResendOtp_SupersedesPreviousOtp() {
        EmailVerification existing = new EmailVerification();
        existing.setEmail(testEmail);
        existing.setPurpose(VerificationPurpose.ACCOUNT_REGISTRATION);
        existing.setOtpHash(passwordEncoder.encode("111111"));
        existing.setAttempts(3);
        existing.setLastSentAt(LocalDateTime.now().minusSeconds(70)); // cooldown passed

        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.empty());
        when(verificationRepository.findByEmailAndPurposeForUpdate(testEmail, VerificationPurpose.ACCOUNT_REGISTRATION))
                .thenReturn(Optional.of(existing));

        VerificationResponseDto response = verificationService.resendOtp(
                testEmail,
                VerificationPurpose.ACCOUNT_REGISTRATION
        );

        assertTrue(response.isSuccess());
        assertEquals(0, existing.getAttempts(), "Attempts must be reset to 0 for replacement OTP");
        assertNotNull(existing.getOtpHash());
        assertFalse(passwordEncoder.matches("111111", existing.getOtpHash()), "Old OTP must be superseded");
        assertNull(existing.getVerifiedAt());
    }
}
