package com.alumni.management.jobpost;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.alumni.management.jobpost.dto.JobDto;
import com.alumni.management.jobpost.entity.Job;
import com.alumni.management.jobpost.repository.JobRepository;
import com.alumni.management.jobpost.service.JobService;
import com.alumni.management.role.entity.Role;
import com.alumni.management.user.entity.User;
import com.alumni.management.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class JobServiceTest {

	@Mock
	private JobRepository jobRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private com.alumni.management.alumni.repository.AlumniProfileRepository alumniProfileRepository;

	@Mock
	private com.alumni.management.faculty.repository.FacultyRepository facultyRepository;

	@InjectMocks
	private JobService jobService;

	private User alumniUser;
	private Role alumniRole;

	@BeforeEach
	void setUp() {
		alumniRole = new Role("ALUMNI");
		alumniRole.setId(2L);
		alumniUser = new User(42L, "Jane Alumni", "jane.alumni@example.com", "password", alumniRole);

		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(alumniUser.getEmail(), "password")
		);
	}

	@Test
	void testGetMyJobsReturnsUserScopedJobs() {
		when(userRepository.findByEmail(alumniUser.getEmail())).thenReturn(Optional.of(alumniUser));

		Job job1 = new Job();
		job1.setId(101L);
		job1.setUser(alumniUser);
		job1.setCompanyName("Tech Corp");
		job1.setJobTitle("Software Engineer");
		job1.setLocation("Ahmedabad");
		job1.setSalary("12 LPA");

		when(jobRepository.findByUserId(42L)).thenReturn(List.of(job1));
		when(alumniProfileRepository.findByUserId(42L)).thenReturn(Optional.empty());

		List<JobDto> myJobs = jobService.getMyJobs();

		assertNotNull(myJobs);
		assertEquals(1, myJobs.size());
		assertEquals("Software Engineer", myJobs.get(0).getJobTitle());
		assertEquals("Tech Corp", myJobs.get(0).getCompanyName());
		assertEquals(alumniUser.getEmail(), myJobs.get(0).getUserEmail());
		verify(jobRepository).findByUserId(42L);
	}

	@Test
	void testGetMyJobsWhenUserHasNoJobsReturnsEmptyList() {
		when(userRepository.findByEmail(alumniUser.getEmail())).thenReturn(Optional.of(alumniUser));
		when(jobRepository.findByUserId(42L)).thenReturn(Collections.emptyList());

		List<JobDto> myJobs = jobService.getMyJobs();

		assertNotNull(myJobs);
		assertTrue(myJobs.isEmpty());
		verify(jobRepository).findByUserId(42L);
	}

	@Test
	void testAdminCanUpdateAnyJob_Success() {
		Role adminRole = new Role("ADMIN");
		adminRole.setId(1L);
		User adminUser = new User(99L, "System Admin", "admin@gls.edu.in", "pass", adminRole);

		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(adminUser.getEmail(), "pass")
		);
		when(userRepository.findByEmail(adminUser.getEmail())).thenReturn(Optional.of(adminUser));

		Job existing = new Job();
		existing.setId(200L);
		existing.setUser(alumniUser); // Job posted by Alumni
		existing.setCompanyName("Old Company");
		existing.setJobTitle("Old Title");
		existing.setLocation("Surat");
		existing.setSalary("7 LPA");
		existing.setJobDescription("Old description that has more than 10 characters.");
		existing.setSkillsRequired("Java, SQL");
		existing.setExperienceRequired("2-3 Years");
		existing.setJoiningType("Immediate");
		existing.setJobType("Full Time");
		existing.setLastDateToApply(java.time.LocalDate.now().plusDays(30));

		when(jobRepository.findById(200L)).thenReturn(Optional.of(existing));

		JobDto updateDto = new JobDto();
		updateDto.setCompanyName("New Company Ltd");
		updateDto.setJobTitle("Lead Architect");
		updateDto.setLocation("Ahmedabad");
		updateDto.setSalary("15 LPA");
		updateDto.setJobDescription("Updated comprehensive architecture description.");
		updateDto.setSkillsRequired("Java, Spring, Microservices");
		updateDto.setExperienceRequired("5+ Years");
		updateDto.setJoiningType("15 Days");
		updateDto.setJobType("Full Time");
		updateDto.setLastDateToApply(java.time.LocalDate.now().plusDays(45));

		String result = jobService.updateJob(200L, updateDto);

		assertTrue(result.contains("Job post updated successfully"));
		assertEquals("New Company Ltd", existing.getCompanyName());
		assertEquals("Lead Architect", existing.getJobTitle());
		assertEquals("Ahmedabad", existing.getLocation());
		assertEquals("15 LPA", existing.getSalary());
		assertEquals(alumniUser.getId(), existing.getUser().getId(), "Original job owner must be preserved");
		verify(jobRepository).save(existing);
	}

	@Test
	void testOwnerAlumniCanUpdateOwnJob_Success() {
		when(userRepository.findByEmail(alumniUser.getEmail())).thenReturn(Optional.of(alumniUser));

		Job existing = new Job();
		existing.setId(201L);
		existing.setUser(alumniUser);
		existing.setCompanyName("Alumni Company");
		existing.setJobTitle("Dev");
		existing.setLocation("Vadodara");
		existing.setSalary("4.5 LPA");
		existing.setJobDescription("Original description of developer role.");
		existing.setSkillsRequired("Flutter, Dart");
		existing.setExperienceRequired("1 Year");
		existing.setLastDateToApply(java.time.LocalDate.now().plusDays(20));

		when(jobRepository.findById(201L)).thenReturn(Optional.of(existing));

		JobDto updateDto = new JobDto();
		updateDto.setCompanyName("Alumni Company Updated");
		updateDto.setJobTitle("Senior Flutter Developer");
		updateDto.setLocation("Vadodara");
		updateDto.setSalary("6.5 LPA");
		updateDto.setJobDescription("Updated Flutter developer role with new requirements.");
		updateDto.setSkillsRequired("Flutter, Dart, Clean Architecture");
		updateDto.setExperienceRequired("2 Years");
		updateDto.setJoiningType("Immediate");
		updateDto.setJobType("Full Time");
		updateDto.setLastDateToApply(java.time.LocalDate.now().plusDays(25));

		String result = jobService.updateJob(201L, updateDto);

		assertTrue(result.contains("Job post updated successfully"));
		assertEquals("Senior Flutter Developer", existing.getJobTitle());
		assertEquals("6.5 LPA", existing.getSalary());
		verify(jobRepository).save(existing);
	}

	@Test
	void testUnauthorizedUserCannotUpdateJob_ThrowsSecurityException() {
		Role otherAlumniRole = new Role("ALUMNI");
		otherAlumniRole.setId(2L);
		User otherUser = new User(88L, "Other User", "other@example.com", "pass", otherAlumniRole);

		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(otherUser.getEmail(), "pass")
		);
		when(userRepository.findByEmail(otherUser.getEmail())).thenReturn(Optional.of(otherUser));

		Job existing = new Job();
		existing.setId(202L);
		existing.setUser(alumniUser); // Belong to Jane Alumni, not otherUser

		when(jobRepository.findById(202L)).thenReturn(Optional.of(existing));

		JobDto updateDto = new JobDto();
		updateDto.setCompanyName("Hacked Co");

		org.junit.jupiter.api.Assertions.assertThrows(SecurityException.class, () -> {
			jobService.updateJob(202L, updateDto);
		});
	}

	@Test
	void testUpdateJob_MissingRequiredFields_ThrowsIllegalArgumentException() {
		when(userRepository.findByEmail(alumniUser.getEmail())).thenReturn(Optional.of(alumniUser));

		Job existing = new Job();
		existing.setId(203L);
		existing.setUser(alumniUser);

		when(jobRepository.findById(203L)).thenReturn(Optional.of(existing));

		JobDto updateDto = new JobDto();
		// Empty company name
		updateDto.setCompanyName("   ");

		org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> {
			jobService.updateJob(203L, updateDto);
		});
	}
}
