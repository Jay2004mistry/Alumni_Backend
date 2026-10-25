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
}
