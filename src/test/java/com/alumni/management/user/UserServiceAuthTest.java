package com.alumni.management.user;

import com.alumni.management.exception.ResourceNotFoundException;
import com.alumni.management.role.entity.Role;
import com.alumni.management.role.repository.RoleRepository;
import com.alumni.management.security.JwtUtil;
import com.alumni.management.user.dto.LoginRequestDto;
import com.alumni.management.user.dto.LoginResponseDto;
import com.alumni.management.user.entity.User;
import com.alumni.management.user.repository.UserRepository;
import com.alumni.management.user.service.UserService;
import com.alumni.management.verification.dto.VerificationResponseDto;
import com.alumni.management.verification.enums.VerificationPurpose;
import com.alumni.management.verification.exception.AccountNotVerifiedException;
import com.alumni.management.verification.exception.EmailDeliveryException;
import com.alumni.management.verification.exception.ResendCooldownException;
import com.alumni.management.verification.exception.UserAlreadyExistsException;
import com.alumni.management.verification.service.VerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceAuthTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private VerificationService verificationService;

    @Mock
    private com.alumni.management.alumni.repository.AlumniProfileRepository alumniProfileRepository;

    @Mock
    private com.alumni.management.student.repository.StudentProfileRepository studentProfileRepository;

    @Mock
    private com.alumni.management.faculty.repository.FacultyRepository facultyRepository;

    @Spy
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @InjectMocks
    private UserService userService;

    private final String testEmail = "alumni@gls.edu.in";
    private final String rawPassword = "SecurePassword123!";
    private Role alumniRole;

    @BeforeEach
    void setUp() {
        alumniRole = new Role();
        alumniRole.setId(1L);
        alumniRole.setRoleName("ALUMNI");
    }

    @Test
    @DisplayName("Unverified account must be rejected at login without issuing JWT and triggers OTP dispatch")
    void testLogin_UnverifiedAccount_Rejected() {
        User unverifiedUser = new User();
        unverifiedUser.setEmail(testEmail);
        unverifiedUser.setPassword(passwordEncoder.encode(rawPassword));
        unverifiedUser.setRole(alumniRole);
        unverifiedUser.setVerified(false);

        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(unverifiedUser));
        when(verificationService.sendVerificationOtp(testEmail, VerificationPurpose.ACCOUNT_ACTIVATION))
                .thenReturn(VerificationResponseDto.builder()
                        .success(true)
                        .message("Verification code has been sent to your email.")
                        .email(testEmail)
                        .cooldownSeconds(60)
                        .build());

        LoginRequestDto request = new LoginRequestDto();
        request.setEmail(testEmail);
        request.setPassword(rawPassword);

        AccountNotVerifiedException ex = assertThrows(AccountNotVerifiedException.class, () ->
                userService.login(request)
        );

        assertTrue(ex.getMessage().contains("Account is not verified"));
        assertTrue(ex.isOtpDispatched());
        assertEquals(60, ex.getCooldownSeconds());
        assertEquals(testEmail, ex.getEmail());
        assertEquals(VerificationPurpose.ACCOUNT_ACTIVATION, ex.getPurpose());
        verify(jwtUtil, never()).generateToken(anyString());
        verify(verificationService, times(1)).sendVerificationOtp(testEmail, VerificationPurpose.ACCOUNT_ACTIVATION);
    }

    @Test
    @DisplayName("Unverified account login during cooldown carries remaining seconds without crashing")
    void testLogin_UnverifiedAccount_DuringCooldown_CarriesCooldown() {
        User unverifiedUser = new User();
        unverifiedUser.setEmail(testEmail);
        unverifiedUser.setPassword(passwordEncoder.encode(rawPassword));
        unverifiedUser.setRole(alumniRole);
        unverifiedUser.setVerified(false);

        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(unverifiedUser));
        when(verificationService.sendVerificationOtp(testEmail, VerificationPurpose.ACCOUNT_ACTIVATION))
                .thenThrow(new ResendCooldownException("Please wait 42 seconds before requesting a new code.", 42));

        LoginRequestDto request = new LoginRequestDto();
        request.setEmail(testEmail);
        request.setPassword(rawPassword);

        AccountNotVerifiedException ex = assertThrows(AccountNotVerifiedException.class, () ->
                userService.login(request)
        );

        assertFalse(ex.isOtpDispatched());
        assertEquals(42, ex.getCooldownSeconds());
        assertTrue(ex.getMessage().contains("42 seconds"));
        verify(jwtUtil, never()).generateToken(anyString());
    }

    @Test
    @DisplayName("Unverified account login when email delivery fails carries safe technical message")
    void testLogin_UnverifiedAccount_EmailDeliveryFailure_CarriesSafeError() {
        User unverifiedUser = new User();
        unverifiedUser.setEmail(testEmail);
        unverifiedUser.setPassword(passwordEncoder.encode(rawPassword));
        unverifiedUser.setRole(alumniRole);
        unverifiedUser.setVerified(false);

        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(unverifiedUser));
        when(verificationService.sendVerificationOtp(testEmail, VerificationPurpose.ACCOUNT_ACTIVATION))
                .thenThrow(new EmailDeliveryException("Failed to send verification email. Please try again later."));

        LoginRequestDto request = new LoginRequestDto();
        request.setEmail(testEmail);
        request.setPassword(rawPassword);

        AccountNotVerifiedException ex = assertThrows(AccountNotVerifiedException.class, () ->
                userService.login(request)
        );

        assertFalse(ex.isOtpDispatched());
        assertEquals(0, ex.getCooldownSeconds());
        assertTrue(ex.getMessage().contains("Account is not verified"));
        verify(jwtUtil, never()).generateToken(anyString());
    }

    @Test
    @DisplayName("Verified account logs in successfully and receives JWT token without requiring OTP")
    void testLogin_VerifiedAccount_Succeeds() {
        User verifiedUser = new User();
        verifiedUser.setEmail(testEmail);
        verifiedUser.setName("John Doe");
        verifiedUser.setPassword(passwordEncoder.encode(rawPassword));
        verifiedUser.setRole(alumniRole);
        verifiedUser.setVerified(true);

        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(verifiedUser));
        when(jwtUtil.generateToken(testEmail)).thenReturn("mock.jwt.token");

        LoginRequestDto request = new LoginRequestDto();
        request.setEmail(testEmail);
        request.setPassword(rawPassword);

        LoginResponseDto response = userService.login(request);

        assertNotNull(response);
        assertEquals("mock.jwt.token", response.getToken());
        assertEquals("John Doe", response.getName());
        assertEquals("ALUMNI", response.getRole());

        // Verify normal login does NOT trigger OTP verification
        verify(verificationService, never()).sendVerificationOtp(anyString(), any());
    }

    @Test
    @DisplayName("Login with invalid password fails before checking verification status")
    void testLogin_InvalidPassword_ThrowsResourceNotFoundException() {
        User user = new User();
        user.setEmail(testEmail);
        user.setPassword(passwordEncoder.encode("DifferentPassword!"));
        user.setRole(alumniRole);
        user.setVerified(false);

        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(user));

        LoginRequestDto request = new LoginRequestDto();
        request.setEmail(testEmail);
        request.setPassword("WrongPassword!");

        assertThrows(ResourceNotFoundException.class, () -> userService.login(request));
        verify(jwtUtil, never()).generateToken(anyString());
        verify(verificationService, never()).sendVerificationOtp(anyString(), any());
    }

    @Test
    @DisplayName("Login with non-existent email throws ResourceNotFoundException")
    void testLogin_UnknownEmail_ThrowsResourceNotFoundException() {
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.empty());

        LoginRequestDto request = new LoginRequestDto();
        request.setEmail(testEmail);
        request.setPassword(rawPassword);

        assertThrows(ResourceNotFoundException.class, () -> userService.login(request));
        verify(jwtUtil, never()).generateToken(anyString());
        verify(verificationService, never()).sendVerificationOtp(anyString(), any());
    }

    @Test
    @DisplayName("Creating user sets verified to false and triggers OTP dispatch")
    void testCreateUser_NewUser_UnverifiedAndDispatchesOtp() {
        User newUser = new User();
        newUser.setEmail(testEmail);
        newUser.setName("New Student");
        newUser.setPassword(rawPassword);

        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.empty());
        when(roleRepository.findByRoleName("ALUMNI")).thenReturn(Optional.of(alumniRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(10L);
            return u;
        });

        User result = userService.createUser(newUser);

        assertNotNull(result);
        assertFalse(result.isVerified(), "New registered user must be unverified");
        assertTrue(passwordEncoder.matches(rawPassword, result.getPassword()), "Password must be BCrypt hashed");
        verify(verificationService, times(1)).sendVerificationOtp(testEmail, VerificationPurpose.ACCOUNT_REGISTRATION);
    }

    @Test
    @DisplayName("Registering with an already verified email throws UserAlreadyExistsException")
    void testCreateUser_AlreadyVerified_ThrowsUserAlreadyExistsException() {
        User existingVerified = new User();
        existingVerified.setEmail(testEmail);
        existingVerified.setVerified(true);

        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(existingVerified));

        User attempt = new User();
        attempt.setEmail(testEmail);
        attempt.setPassword(rawPassword);

        assertThrows(UserAlreadyExistsException.class, () -> userService.createUser(attempt));
        verify(verificationService, never()).sendVerificationOtp(anyString(), any());
    }

    @Test
    @DisplayName("Registering with existing unverified account re-issues OTP and updates details")
    void testCreateUser_ExistingUnverified_ReissuesOtp() {
        User existingUnverified = new User();
        existingUnverified.setId(42L);
        existingUnverified.setEmail(testEmail);
        existingUnverified.setVerified(false);

        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(existingUnverified));
        when(roleRepository.findByRoleName("ALUMNI")).thenReturn(Optional.of(alumniRole));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User updateAttempt = new User();
        updateAttempt.setEmail(testEmail);
        updateAttempt.setName("Updated Name");
        updateAttempt.setPassword("NewSecretPassword!");

        User result = userService.createUser(updateAttempt);

        assertNotNull(result);
        assertEquals(42L, result.getId());
        assertEquals("Updated Name", result.getName());
        assertFalse(result.isVerified());
        assertTrue(passwordEncoder.matches("NewSecretPassword!", result.getPassword()));

        verify(verificationService, times(1)).sendVerificationOtp(testEmail, VerificationPurpose.ACCOUNT_REGISTRATION);
    }

    @Test
    @DisplayName("deleteUser on ADMIN user is rejected with IllegalArgumentException")
    void deleteUser_AdminRole_ThrowsIllegalArgumentException() {
        Role adminRole = new Role("ADMIN");
        User adminUser = new User();
        adminUser.setId(99L);
        adminUser.setEmail("admin@alumni.com");
        adminUser.setRole(adminRole);

        when(userRepository.findById(99L)).thenReturn(Optional.of(adminUser));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            userService.deleteUser(99L);
        });

        assertEquals("Admin accounts cannot be deleted", ex.getMessage());
        verify(userRepository, never()).delete(adminUser);
        verify(userRepository, never()).deleteById(99L);
    }

    @Test
    @DisplayName("deleteUser on ordinary non-admin user succeeds")
    void deleteUser_OrdinaryUser_Succeeds() {
        Role studentRole = new Role("STUDENT");
        User studentUser = new User();
        studentUser.setId(105L);
        studentUser.setEmail("student@alumni.com");
        studentUser.setRole(studentRole);

        when(userRepository.findById(105L)).thenReturn(Optional.of(studentUser));

        userService.deleteUser(105L);

        verify(userRepository, times(1)).delete(studentUser);
    }

    @Test
    @DisplayName("refreshToken with valid authenticated session returns fresh token")
    void testRefreshToken_Success() {
        User user = new User();
        user.setEmail(testEmail);
        user.setName("Alice Alumni");
        user.setRole(alumniRole);

        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(testEmail)).thenReturn("refreshed.new.token");

        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(testEmail, null)
        );

        java.util.Map<String, String> result = userService.refreshToken();

        assertNotNull(result);
        assertEquals("refreshed.new.token", result.get("token"));
        assertEquals("Alice Alumni", result.get("name"));
        assertEquals("ALUMNI", result.get("role"));
    }

    @Test
    @DisplayName("processForgotPassword dispatches OTP via VerificationService")
    void testProcessForgotPassword_Success() {
        User user = new User();
        user.setEmail(testEmail);
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(user));

        com.alumni.management.user.dto.ForgotPasswordRequestDto req = new com.alumni.management.user.dto.ForgotPasswordRequestDto();
        req.setEmail(testEmail);

        java.util.Map<String, String> response = userService.processForgotPassword(req);
        assertNotNull(response);
        assertEquals(testEmail, response.get("email"));
        verify(verificationService).sendVerificationOtp(testEmail, VerificationPurpose.PASSWORD_RESET);
    }

    @Test
    @DisplayName("resetPassword verifies OTP via VerificationService and hashes new password")
    void testResetPassword_Success() {
        User user = new User();
        user.setEmail(testEmail);
        user.setPassword(passwordEncoder.encode("OldPassword123!"));
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(user));

        com.alumni.management.user.dto.ResetPasswordRequestDto req = new com.alumni.management.user.dto.ResetPasswordRequestDto();
        req.setEmail(testEmail);
        req.setOtp("123456");
        req.setNewPassword("NewSecurePassword456!");

        java.util.Map<String, String> response = userService.resetPassword(req);
        assertNotNull(response);
        assertEquals("Password reset successfully", response.get("message"));

        verify(verificationService).verifyOtp(testEmail, "123456", VerificationPurpose.PASSWORD_RESET);
        verify(userRepository).save(argThat(savedUser ->
            passwordEncoder.matches("NewSecurePassword456!", savedUser.getPassword())
        ));
    }
}
