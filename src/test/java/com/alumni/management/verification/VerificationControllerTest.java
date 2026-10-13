package com.alumni.management.verification;

import com.alumni.management.verification.controller.VerificationController;
import com.alumni.management.verification.dto.SendOtpRequestDto;
import com.alumni.management.verification.dto.VerificationResponseDto;
import com.alumni.management.verification.dto.VerifyOtpRequestDto;
import com.alumni.management.verification.enums.VerificationPurpose;
import com.alumni.management.verification.service.VerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificationControllerTest {

    @Mock
    private VerificationService verificationService;

    private VerificationController controller;

    private final String testEmail = "alumni@gls.edu.in";

    @BeforeEach
    void setUp() {
        controller = new VerificationController(verificationService);
    }

    @Test
    @DisplayName("POST /api/auth/verification/send delegates to VerificationService and returns 200 OK")
    void testSendOtp_ReturnsOk() {
        SendOtpRequestDto request = SendOtpRequestDto.builder()
                .email(testEmail)
                .purpose(VerificationPurpose.ACCOUNT_REGISTRATION)
                .build();

        VerificationResponseDto expectedResponse = VerificationResponseDto.builder()
                .success(true)
                .message("Verification code has been sent to your email.")
                .email(testEmail)
                .cooldownSeconds(60)
                .build();

        when(verificationService.sendVerificationOtp(testEmail, VerificationPurpose.ACCOUNT_REGISTRATION))
                .thenReturn(expectedResponse);

        ResponseEntity<VerificationResponseDto> response = controller.sendOtp(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals(60, response.getBody().getCooldownSeconds());
        verify(verificationService).sendVerificationOtp(testEmail, VerificationPurpose.ACCOUNT_REGISTRATION);
    }

    @Test
    @DisplayName("POST /api/auth/verification/verify delegates to VerificationService and returns 200 OK")
    void testVerifyOtp_ReturnsOk() {
        VerifyOtpRequestDto request = VerifyOtpRequestDto.builder()
                .email(testEmail)
                .otp("123456")
                .purpose(VerificationPurpose.ACCOUNT_REGISTRATION)
                .build();

        VerificationResponseDto expectedResponse = VerificationResponseDto.builder()
                .success(true)
                .message("Email verified successfully.")
                .email(testEmail)
                .build();

        when(verificationService.verifyOtp(testEmail, "123456", VerificationPurpose.ACCOUNT_REGISTRATION))
                .thenReturn(expectedResponse);

        ResponseEntity<VerificationResponseDto> response = controller.verifyOtp(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        verify(verificationService).verifyOtp(testEmail, "123456", VerificationPurpose.ACCOUNT_REGISTRATION);
    }

    @Test
    @DisplayName("POST /api/auth/verification/resend delegates to VerificationService and returns 200 OK")
    void testResendOtp_ReturnsOk() {
        SendOtpRequestDto request = SendOtpRequestDto.builder()
                .email(testEmail)
                .purpose(VerificationPurpose.ACCOUNT_REGISTRATION)
                .build();

        VerificationResponseDto expectedResponse = VerificationResponseDto.builder()
                .success(true)
                .message("A new verification code has been sent to your email.")
                .email(testEmail)
                .cooldownSeconds(60)
                .build();

        when(verificationService.resendOtp(testEmail, VerificationPurpose.ACCOUNT_REGISTRATION))
                .thenReturn(expectedResponse);

        ResponseEntity<VerificationResponseDto> response = controller.resendOtp(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        verify(verificationService).resendOtp(testEmail, VerificationPurpose.ACCOUNT_REGISTRATION);
    }
}
