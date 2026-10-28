package com.alumni.management.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

import com.alumni.management.alumni.entity.AlumniProfile;
import com.alumni.management.alumni.repository.AlumniProfileRepository;
import com.alumni.management.config.WebConfig;
import com.alumni.management.faculty.entity.FacultyProfile;
import com.alumni.management.faculty.repository.FacultyRepository;
import com.alumni.management.role.entity.Role;
import com.alumni.management.security.FileUploadSecurityUtil;
import com.alumni.management.student.entity.StudentProfile;
import com.alumni.management.student.repository.StudentProfileRepository;
import com.alumni.management.user.dto.UserResponseDto;
import com.alumni.management.user.entity.User;
import com.alumni.management.user.repository.UserRepository;
import com.alumni.management.user.service.UserService;

@ExtendWith(MockitoExtension.class)
public class ProfilePhotoLifecycleBackendTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AlumniProfileRepository alumniProfileRepository;

    @Mock
    private StudentProfileRepository studentProfileRepository;

    @Mock
    private FacultyRepository facultyRepository;

    @InjectMocks
    private UserService userService;

    private static final byte[] VALID_PNG = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
    private static final byte[] VALID_JPEG = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0};

    @BeforeEach
    void setupAuth() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user@test.com", "pass")
        );
    }

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testStudentProfilePhotoUploadUpdatesStudentProfileEntity() throws IOException {
        User studentUser = new User();
        studentUser.setId(10L);
        studentUser.setEmail("user@test.com");
        studentUser.setName("Student Tester");
        studentUser.setRole(new Role("STUDENT"));

        StudentProfile existingProfile = new StudentProfile();
        existingProfile.setId(1L);
        existingProfile.setUser(studentUser);

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(studentUser));
        when(studentProfileRepository.findByUserId(10L)).thenReturn(Optional.of(existingProfile));

        MockMultipartFile file = new MockMultipartFile("image", "avatar.png", "image/png", VALID_PNG);
        String uploadedUrl = userService.uploadProfileImage(file);

        assertNotNull(uploadedUrl);
        assertTrue(uploadedUrl.startsWith("/uploads/profiles/"));
        assertEquals(uploadedUrl, existingProfile.getProfilePictureUrl());
        verify(studentProfileRepository, atLeastOnce()).save(existingProfile);
    }

    @Test
    void testAlumniProfilePhotoUploadUpdatesAlumniProfileEntity() throws IOException {
        User alumniUser = new User();
        alumniUser.setId(20L);
        alumniUser.setEmail("user@test.com");
        alumniUser.setName("Alumni Tester");
        alumniUser.setRole(new Role("ALUMNI"));

        AlumniProfile existingProfile = new AlumniProfile();
        existingProfile.setId(2L);
        existingProfile.setUser(alumniUser);

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(alumniUser));
        when(alumniProfileRepository.findByUserId(20L)).thenReturn(Optional.of(existingProfile));

        MockMultipartFile file = new MockMultipartFile("image", "avatar.jpg", "image/jpeg", VALID_JPEG);
        String uploadedUrl = userService.uploadProfileImage(file);

        assertNotNull(uploadedUrl);
        assertTrue(uploadedUrl.startsWith("/uploads/profiles/"));
        assertEquals(uploadedUrl, existingProfile.getProfilePictureUrl());
        verify(alumniProfileRepository, atLeastOnce()).save(existingProfile);
    }

    @Test
    void testFacultyProfilePhotoUploadUpdatesFacultyProfileEntity() throws IOException {
        User facultyUser = new User();
        facultyUser.setId(30L);
        facultyUser.setEmail("user@test.com");
        facultyUser.setName("Faculty Tester");
        facultyUser.setRole(new Role("FACULTY"));

        FacultyProfile existingProfile = new FacultyProfile();
        existingProfile.setId(3L);
        existingProfile.setUser(facultyUser);

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(facultyUser));
        when(facultyRepository.findByUserId(30L)).thenReturn(Optional.of(existingProfile));

        MockMultipartFile file = new MockMultipartFile("image", "avatar.png", "image/png", VALID_PNG);
        String uploadedUrl = userService.uploadProfileImage(file);

        assertNotNull(uploadedUrl);
        assertTrue(uploadedUrl.startsWith("/uploads/profiles/"));
        assertEquals(uploadedUrl, existingProfile.getProfilePictureUrl());
        verify(facultyRepository, atLeastOnce()).save(existingProfile);
    }

    @Test
    void testReplacingProfilePhotoDeletesPreviousFileFromDisk() throws IOException {
        User studentUser = new User();
        studentUser.setId(10L);
        studentUser.setEmail("user@test.com");
        studentUser.setRole(new Role("STUDENT"));

        // Create an existing temporary file in uploadRoot
        Path uploadRoot = FileUploadSecurityUtil.getUploadRoot();
        Path profilesDir = uploadRoot.resolve("uploads/profiles");
        Files.createDirectories(profilesDir);
        Path oldPhotoFile = Files.createTempFile(profilesDir, "old_test_", ".png");
        String oldRelativeUrl = "/uploads/profiles/" + oldPhotoFile.getFileName().toString();

        StudentProfile existingProfile = new StudentProfile();
        existingProfile.setId(1L);
        existingProfile.setUser(studentUser);
        existingProfile.setProfilePictureUrl(oldRelativeUrl);

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(studentUser));
        when(studentProfileRepository.findByUserId(10L)).thenReturn(Optional.of(existingProfile));

        assertTrue(Files.exists(oldPhotoFile));

        MockMultipartFile newFile = new MockMultipartFile("image", "new_avatar.png", "image/png", VALID_PNG);
        String newUrl = userService.uploadProfileImage(newFile);

        assertNotNull(newUrl);
        assertNotEquals(oldRelativeUrl, newUrl);
        // The old photo file should now be deleted
        assertFalse(Files.exists(oldPhotoFile), "Old photo file should be safely deleted after replacement");
    }

    @Test
    void testUnauthenticatedUploadThrowsSecurityException() {
        SecurityContextHolder.clearContext();

        MockMultipartFile file = new MockMultipartFile("image", "avatar.png", "image/png", VALID_PNG);
        assertThrows(SecurityException.class, () -> userService.uploadProfileImage(file));
    }

    @Test
    void testUserResponseDtoCarriesProfilePictureUrl() {
        User user = new User();
        user.setId(50L);
        user.setEmail("user@test.com");
        user.setName("John Doe");
        user.setRole(new Role("STUDENT"));

        StudentProfile profile = new StudentProfile();
        profile.setDepartment("MCA");
        profile.setProfilePictureUrl("/uploads/profiles/sample.jpg");

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(studentProfileRepository.findByUserId(50L)).thenReturn(Optional.of(profile));

        UserResponseDto dto = userService.getCurrentUserProfile();
        assertNotNull(dto);
        assertEquals("/uploads/profiles/sample.jpg", dto.getProfilePictureUrl());
        assertEquals("MCA", dto.getDepartment());
    }

    @Test
    void testWebConfigRegistersUploadRootResourceHandler() {
        WebConfig webConfig = new WebConfig();
        ResourceHandlerRegistry registry = mock(ResourceHandlerRegistry.class);
        ResourceHandlerRegistration registration = mock(ResourceHandlerRegistration.class);

        when(registry.addResourceHandler("/uploads/**")).thenReturn(registration);
        when(registration.addResourceLocations(any(String[].class))).thenReturn(registration);
        when(registration.setCachePeriod(anyInt())).thenReturn(registration);

        webConfig.addResourceHandlers(registry);

        verify(registry).addResourceHandler("/uploads/**");
        verify(registration).addResourceLocations(any(String[].class));
    }
}
