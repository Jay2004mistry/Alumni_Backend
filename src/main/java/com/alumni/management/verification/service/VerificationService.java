package com.alumni.management.verification.service;

import com.alumni.management.email.service.EmailService;
import com.alumni.management.user.entity.User;
import com.alumni.management.user.repository.UserRepository;
import com.alumni.management.verification.dto.VerificationResponseDto;
import com.alumni.management.verification.entity.EmailVerification;
import com.alumni.management.verification.enums.VerificationPurpose;
import com.alumni.management.verification.exception.*;
import com.alumni.management.verification.repository.EmailVerificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

@Service
public class VerificationService {

    private static final Logger logger = LoggerFactory.getLogger(VerificationService.class);
    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 5;
    private static final int RESEND_COOLDOWN_SECONDS = 60;

    private final EmailVerificationRepository verificationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public VerificationService(
            EmailVerificationRepository verificationRepository,
            UserRepository userRepository,
            EmailService emailService,
            PasswordEncoder passwordEncoder
    ) {
        this.verificationRepository = verificationRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    private boolean isAccountVerification(VerificationPurpose purpose) {
        return purpose == VerificationPurpose.ACCOUNT_REGISTRATION || purpose == VerificationPurpose.ACCOUNT_ACTIVATION;
    }

    /**
     * Send or re-issue verification OTP.
     * Enforces normalization, cooldowns, secure generation, and hashing.
     */
    @Transactional
    public VerificationResponseDto sendVerificationOtp(String rawEmail, VerificationPurpose purpose) {
        String email = normalizeEmail(rawEmail);

        // If purpose is ACCOUNT_REGISTRATION or ACCOUNT_ACTIVATION and user is already registered & verified, reject
        if (isAccountVerification(purpose)) {
            Optional<User> existingUser = userRepository.findByEmail(email);
            if (existingUser.isPresent() && existingUser.get().isVerified()) {
                throw new UserAlreadyExistsException("This email is already registered and verified. Please log in.");
            }
        }

        EmailVerification verification = verificationRepository
                .findByEmailAndPurposeForUpdate(email, purpose)
                .orElse(null);

        // If not found directly, check for alternate account verification purpose
        if (verification == null && isAccountVerification(purpose)) {
            VerificationPurpose alternate = (purpose == VerificationPurpose.ACCOUNT_ACTIVATION)
                    ? VerificationPurpose.ACCOUNT_REGISTRATION
                    : VerificationPurpose.ACCOUNT_ACTIVATION;
            verification = verificationRepository
                    .findByEmailAndPurposeForUpdate(email, alternate)
                    .orElse(null);
            if (verification != null) {
                verification.setPurpose(purpose);
            }
        }

        LocalDateTime now = LocalDateTime.now();

        // Check resend cooldown
        if (verification != null && verification.getLastSentAt() != null) {
            long secondsSinceLastSent = Duration.between(verification.getLastSentAt(), now).getSeconds();
            if (secondsSinceLastSent < RESEND_COOLDOWN_SECONDS) {
                long remaining = RESEND_COOLDOWN_SECONDS - secondsSinceLastSent;
                throw new ResendCooldownException(
                        "Please wait " + remaining + " seconds before requesting a new code.",
                        remaining
                );
            }
        }

        // Generate cryptographically secure 6-digit OTP preserving leading zeros
        String plaintextOtp = generateSecureSixDigitOtp();
        String hashedOtp = passwordEncoder.encode(plaintextOtp);

        if (verification == null) {
            verification = new EmailVerification();
            verification.setEmail(email);
            verification.setPurpose(purpose);
        }

        verification.setOtpHash(hashedOtp);
        verification.setAttempts(0);
        verification.setCreatedAt(now);
        verification.setLastSentAt(now);
        verification.setExpiresAt(now.plusMinutes(OTP_EXPIRY_MINUTES));
        verification.setVerifiedAt(null);

        verificationRepository.save(verification);

        // Deliver email via EmailService
        try {
            emailService.sendVerificationOtpEmail(email, plaintextOtp, purpose);
        } catch (EmailDeliveryException e) {
            // Allow immediate retry if email failed to send by clearing cooldown
            verification.setLastSentAt(now.minusSeconds(RESEND_COOLDOWN_SECONDS));
            verificationRepository.save(verification);
            throw e;
        }

        logger.info("Generated and dispatched OTP for purpose: {}", purpose);

        return VerificationResponseDto.builder()
                .success(true)
                .message("Verification code has been sent to your email.")
                .email(email)
                .cooldownSeconds(RESEND_COOLDOWN_SECONDS)
                .build();
    }

    /**
     * Validate submitted OTP with attempt limits, expiry, and post-verification actions.
     */
    @Transactional(noRollbackFor = {InvalidOtpException.class, MaxAttemptsExceededException.class})
    public VerificationResponseDto verifyOtp(String rawEmail, String submittedOtp, VerificationPurpose purpose) {
        String email = normalizeEmail(rawEmail);

        if (submittedOtp == null || !submittedOtp.matches("^[0-9]{6}$")) {
            throw new InvalidOtpException("Verification code must be exactly 6 digits.");
        }

        EmailVerification verification = verificationRepository
                .findByEmailAndPurposeForUpdate(email, purpose)
                .orElse(null);

        if (verification == null && isAccountVerification(purpose)) {
            VerificationPurpose alternate = (purpose == VerificationPurpose.ACCOUNT_ACTIVATION)
                    ? VerificationPurpose.ACCOUNT_REGISTRATION
                    : VerificationPurpose.ACCOUNT_ACTIVATION;
            verification = verificationRepository
                    .findByEmailAndPurposeForUpdate(email, alternate)
                    .orElse(null);
        }

        if (verification == null) {
            throw new InvalidOtpException("No verification request found for this email and purpose.");
        }

        // Already consumed
        if (verification.getVerifiedAt() != null) {
            throw new InvalidOtpException("This verification code has already been used.");
        }

        // Max attempts reached
        if (verification.getAttempts() >= MAX_ATTEMPTS) {
            throw new MaxAttemptsExceededException("Maximum verification attempts exceeded. Please request a new code.");
        }

        // Expired
        if (LocalDateTime.now().isAfter(verification.getExpiresAt())) {
            throw new OtpExpiredException("Verification code has expired. Please request a new code.");
        }

        // Check hash
        if (!passwordEncoder.matches(submittedOtp, verification.getOtpHash())) {
            int currentAttempts = verification.getAttempts() + 1;
            verification.setAttempts(currentAttempts);
            verificationRepository.saveAndFlush(verification);

            int remaining = MAX_ATTEMPTS - currentAttempts;
            if (remaining <= 0) {
                throw new MaxAttemptsExceededException("Maximum verification attempts exceeded. Please request a new code.");
            }
            throw new InvalidOtpException("Invalid verification code. " + remaining + " attempts remaining.");
        }

        // Verification successful
        verification.setVerifiedAt(LocalDateTime.now());
        verificationRepository.save(verification);

        // Update corresponding User account if registration or activation
        if (isAccountVerification(purpose) || isAccountVerification(verification.getPurpose())) {
            userRepository.findByEmail(email).ifPresent(user -> {
                user.setVerified(true);
                userRepository.save(user);
                logger.info("User account verified successfully for email: {}", email);
            });
        }

        return VerificationResponseDto.builder()
                .success(true)
                .message("Email verified successfully.")
                .email(email)
                .build();
    }

    /**
     * Resend verification OTP adhering to the 60-second cooldown.
     */
    @Transactional
    public VerificationResponseDto resendOtp(String rawEmail, VerificationPurpose purpose) {
        VerificationResponseDto response = sendVerificationOtp(rawEmail, purpose);
        response.setMessage("A new verification code has been sent to your email.");
        return response;
    }

    /**
     * Cryptographically secure 6-digit OTP generator preserving leading zeros.
     */
    public String generateSecureSixDigitOtp() {
        int code = secureRandom.nextInt(1_000_000);
        return String.format("%06d", code);
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
