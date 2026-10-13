package com.alumni.management.student;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.alumni.management.role.entity.Role;
import com.alumni.management.student.dto.StudentProfileDto;
import com.alumni.management.student.entity.StudentProfile;
import com.alumni.management.student.repository.StudentProfileRepository;
import com.alumni.management.student.service.StudentProfileService;
import com.alumni.management.user.entity.User;
import com.alumni.management.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class StudentProfileServiceTest {

	@Mock
	private StudentProfileRepository profileRepository;

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private StudentProfileService profileService;

	private User studentUser;
	private Role studentRole;

	@BeforeEach
	void setUp() {
		studentRole = new Role("STUDENT");
		studentRole.setId(1L);
		studentUser = new User(10L, "John Student", "john.student@example.com", "password", studentRole);
		studentUser.setUniversityNumber("GLS2026STU001");

		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(studentUser.getEmail(), "password")
		);
	}

	@Test
	void testGetProfileWhenNotYetCreatedReturnsInitializedDto() {
		when(userRepository.findByEmail(studentUser.getEmail())).thenReturn(Optional.of(studentUser));
		when(profileRepository.findByUserId(10L)).thenReturn(Optional.empty());

		StudentProfileDto dto = profileService.getProfile();

		assertNotNull(dto);
		assertEquals("John Student", dto.getUserName());
		assertEquals("john.student@example.com", dto.getEmail());
		assertEquals("GLS2026STU001", dto.getUniversityNumber());
		assertEquals("MCA", dto.getDepartment());
	}

	@Test
	void testGetProfileWhenProfileExists() {
		when(userRepository.findByEmail(studentUser.getEmail())).thenReturn(Optional.of(studentUser));

		StudentProfile existing = new StudentProfile();
		existing.setId(1L);
		existing.setUser(studentUser);
		existing.setDepartment("MCA");
		existing.setDegree("Master of Computer Applications");
		existing.setBatchYear(2026);
		existing.setSemester("Semester 4");
		existing.setSkills("Flutter, Spring Boot");
		existing.setBio("Aspiring Software Architect");

		when(profileRepository.findByUserId(10L)).thenReturn(Optional.of(existing));

		StudentProfileDto dto = profileService.getProfile();

		assertNotNull(dto);
		assertEquals("John Student", dto.getUserName());
		assertEquals("Flutter, Spring Boot", dto.getSkills());
		assertEquals(2026, dto.getBatchYear());
		assertEquals("Semester 4", dto.getSemester());
	}

	@Test
	void testCreateOrUpdateProfileSuccessful() {
		when(userRepository.findByEmail(studentUser.getEmail())).thenReturn(Optional.of(studentUser));
		when(profileRepository.findByUserId(10L)).thenReturn(Optional.empty());
		when(profileRepository.save(any(StudentProfile.class))).thenAnswer(i -> i.getArguments()[0]);

		StudentProfile input = new StudentProfile();
		input.setDepartment("MCA");
		input.setDegree("Master of Computer Applications");
		input.setBatchYear(2026);
		input.setSemester("Semester 4");
		input.setSkills("Flutter, Java, Dart");
		input.setCurrentCity("Ahmedabad");

		String result = profileService.createOrUpdateProfile(input);

		assertEquals("Profile saved successfully", result);
	}
}
