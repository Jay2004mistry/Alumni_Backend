package com.alumni.management.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.alumni.management.admin.dto.AdminStatsDto;
import com.alumni.management.admin.service.AdminService;
import com.alumni.management.alumni.repository.AlumniProfileRepository;
import com.alumni.management.faculty.repository.FacultyRepository;
import com.alumni.management.role.entity.Role;
import com.alumni.management.student.repository.StudentProfileRepository;
import com.alumni.management.user.dto.UserResponseDto;
import com.alumni.management.user.entity.User;
import com.alumni.management.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class AdminServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private AlumniProfileRepository alumniProfileRepository;

	@Mock
	private FacultyRepository facultyRepository;

	@Mock
	private StudentProfileRepository studentProfileRepository;

	@InjectMocks
	private AdminService adminService;

	private Role adminRole;
	private Role alumniRole;
	private Role facultyRole;
	private Role studentRole;

	private User adminUser;
	private User alumniUser;
	private User facultyUser;
	private User studentUser;

	@BeforeEach
	void setUp() {
		adminRole = new Role("ADMIN");
		adminRole.setId(1L);

		alumniRole = new Role("ALUMNI");
		alumniRole.setId(2L);

		facultyRole = new Role("FACULTY");
		facultyRole.setId(3L);

		studentRole = new Role("STUDENT");
		studentRole.setId(4L);

		adminUser = new User(100L, "Main Administrator", "admin@alumni.com", "pass123", adminRole);
		alumniUser = new User(101L, "Vivek Alumni", "vivek@example.com", "pass123", alumniRole);
		facultyUser = new User(102L, "Prof. Sharma", "sharma@example.com", "pass123", facultyRole);
		studentUser = new User(103L, "Rahul Student", "rahul@example.com", "pass123", studentRole);
	}

	@Test
	void testGetAllUsersExcludesAdminAccounts() {
		List<User> mockUsers = Arrays.asList(adminUser, alumniUser, facultyUser, studentUser);
		when(userRepository.findAll()).thenReturn(mockUsers);

		List<UserResponseDto> result = adminService.getAllUsers();

		assertNotNull(result);
		assertEquals(3, result.size());

		// Verify no admin account exists in the returned list
		boolean hasAdmin = result.stream().anyMatch(u -> "ADMIN".equalsIgnoreCase(u.getRoleName()));
		assertEquals(false, hasAdmin, "Admin accounts must be excluded from getAllUsers()");

		// Verify Student, Alumni, and Faculty accounts remain present
		assertTrue(result.stream().anyMatch(u -> "ALUMNI".equalsIgnoreCase(u.getRoleName())));
		assertTrue(result.stream().anyMatch(u -> "FACULTY".equalsIgnoreCase(u.getRoleName())));
		assertTrue(result.stream().anyMatch(u -> "STUDENT".equalsIgnoreCase(u.getRoleName())));
	}

	@Test
	void testGetUserByRoleForAdminReturnsEmptyList() {
		List<UserResponseDto> result = adminService.getUserByRole("ADMIN");

		assertNotNull(result);
		assertTrue(result.isEmpty(), "getUserByRole('ADMIN') must return an empty list");
		verify(userRepository, never()).findByRole_RoleName("ADMIN");
	}

	@Test
	void testGetUserByRoleForAlumniReturnsAlumniAccounts() {
		when(userRepository.findByRole_RoleName("ALUMNI")).thenReturn(List.of(alumniUser));

		List<UserResponseDto> result = adminService.getUserByRole("ALUMNI");

		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals("Vivek Alumni", result.get(0).getName());
		assertEquals("ALUMNI", result.get(0).getRoleName());
	}

	@Test
	void testDeleteAdminAccountIsRejected() {
		when(userRepository.findById(100L)).thenReturn(Optional.of(adminUser));

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			adminService.deleteUser(100L);
		});

		assertEquals("Admin accounts cannot be deleted", ex.getMessage());
		verify(userRepository, never()).delete(adminUser);
	}

	@Test
	void testDeleteOrdinaryUserSucceeds() {
		when(userRepository.findById(103L)).thenReturn(Optional.of(studentUser));

		String message = adminService.deleteUser(103L);

		assertEquals("User deleted successfully", message);
		verify(userRepository).delete(studentUser);
	}

	@Test
	void testGetAdminStatsTotalUsersExcludesAdmins() {
		List<User> mockUsers = Arrays.asList(adminUser, alumniUser, facultyUser, studentUser);
		when(userRepository.findAll()).thenReturn(mockUsers);

		AdminStatsDto stats = adminService.getAdminStats();

		assertNotNull(stats);
		assertEquals(3, stats.getTotalUsers(), "Total users count must equal non-admin accounts count");
		assertEquals(1, stats.getTotalAlumni());
		assertEquals(1, stats.getTotalFaculty());
		assertEquals(1, stats.getTotalStudents());
	}
}
