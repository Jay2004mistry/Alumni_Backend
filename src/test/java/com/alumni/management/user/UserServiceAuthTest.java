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
import com.alumni.management.verification.enums.VerificationPurpose;
import com.alumni.management.verification.exception.AccountNotVerifiedException;
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
    @DisplayName("Unverified account must be rejected at login without issuing JWT")
    void testLogin_UnverifiedAccount_Rejected() {
        User unverifiedUser = new User();
        unverifiedUser.setEmail(testEmail);
        unverifiedUser.setPassword(passwordEncoder.encode(rawPassword));
        unverifiedUser.setRole(alumniRole);
        unverifiedUser.setVerified(false);

        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(unverifiedUser));

        LoginRequestDto request = new LoginRequestDto();
        request.setEmail(testEmail);
        request.setPassword(rawPassword);

        AccountNotVerifiedException ex = assertThrows(AccountNotVerifiedException.class, () ->
                userService.login(request)
        );

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
    @DisplayName("Login with invalid password is rejected")
    void testLogin_InvalidPassword_Rejected() {
        User user = new User();
        user.setEmail(testEmail);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(alumniRole);
        user.setVerified(true);

        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(user));

        LoginRequestDto request = new LoginRequestDto();
        request.setEmail(testEmail);
        request.setPassword("WrongPassword!");

        assertThrows(ResourceNotFoundException.class, () -> userService.login(request));
        verify(jwtUtil, never()).generateToken(anyString());
    }

    @Test
    @DisplayName("Create user initiates verification with verified=false")
    void testCreateUser_NewUser_InitiatesVerification() {
        User newUser = new User();
        newUser.setEmail(testEmail);
        newUser.setName("New Student");
        newUser.setPassword(rawPassword);
        newUser.setRole(alumniRole);

        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.empty());
        when(roleRepository.findByRoleName("ALUMNI")).thenReturn(Optional.of(alumniRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.createUser(newUser);

        assertNotNull(result);
        assertFalse(result.isVerified(), "New user must be unverified initially");
        assertTrue(passwordEncoder.matches(rawPassword, result.getPassword()), "Password must be BCrypt encoded");

        verify(verificationService, times(1)).sendVerificationOtp(testEmail, VerificationPurpose.ACCOUNT_REGISTRATION);
    }

    @Test
    @DisplayName("Registration with already verified email is rejected")
    void testCreateUser_AlreadyVerified_Rejected() {
        User existingUser = new User();
        existingUser.setEmail(testEmail);
        existingUser.setVerified(true);

        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(existingUser));

        User registerAttempt = new User();
        registerAttempt.setEmail(testEmail);
        registerAttempt.setPassword(rawPassword);
        registerAttempt.setRole(alumniRole);

        assertThrows(UserAlreadyExistsException.class, () -> userService.createUser(registerAttempt));
        verify(verificationService, never()).sendVerificationOtp(anyString(), any());
    }

    @Test
    @DisplayName("Duplicate registration with unverified email updates account and re-sends OTP")
    void testCreateUser_UnverifiedExisting_UpdatesAndResendsOtp() {
        User existingUnverified = new User();
        existingUnverified.setId(10L);
        existingUnverified.setEmail(testEmail);
        existingUnverified.setName("Old Name");
        existingUnverified.setPassword(passwordEncoder.encode("oldPassword"));
        existingUnverified.setRole(alumniRole);
        existingUnverified.setVerified(false);

        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(existingUnverified));
        when(roleRepository.findByRoleName("ALUMNI")).thenReturn(Optional.of(alumniRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updatedAttempt = new User();
        updatedAttempt.setEmail(testEmail);
        updatedAttempt.setName("Updated Name");
        updatedAttempt.setPassword("NewSecretPassword!");
        updatedAttempt.setRole(alumniRole);

        User result = userService.createUser(updatedAttempt);

        assertEquals("Updated Name", result.getName());
        assertFalse(result.isVerified());
        assertTrue(passwordEncoder.matches("NewSecretPassword!", result.getPassword()));

        verify(verificationService, times(1)).sendVerificationOtp(testEmail, VerificationPurpose.ACCOUNT_REGISTRATION);
    }
}
