package com.alumni.management.jobpost;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.alumni.management.jobpost.controller.JobController;
import com.alumni.management.jobpost.dto.JobDto;
import com.alumni.management.jobpost.service.JobService;

@ExtendWith(MockitoExtension.class)
public class JobControllerTest {

	@Mock
	private JobService jobService;

	@InjectMocks
	private JobController jobController;

	@Test
	@DisplayName("GET /api/jobs/my delegates to JobService.getMyJobs() and returns user-scoped list")
	void testGetMyJobs_ReturnsUserScopedJobs() {
		JobDto dto = new JobDto();
		dto.setId(101L);
		dto.setJobTitle("Software Engineer");
		dto.setCompanyName("Tech Corp");
		dto.setUserEmail("alumni@gls.edu.in");

		when(jobService.getMyJobs()).thenReturn(List.of(dto));

		List<JobDto> response = jobController.getMyJobs();

		assertNotNull(response);
		assertEquals(1, response.size());
		assertEquals("Software Engineer", response.get(0).getJobTitle());
		assertEquals("alumni@gls.edu.in", response.get(0).getUserEmail());
		verify(jobService).getMyJobs();
	}

	@Test
	@DisplayName("GET /api/jobs/my returns empty list when user has posted no jobs")
	void testGetMyJobs_WhenNoJobs_ReturnsEmptyList() {
		when(jobService.getMyJobs()).thenReturn(Collections.emptyList());

		List<JobDto> response = jobController.getMyJobs();

		assertNotNull(response);
		assertTrue(response.isEmpty());
		verify(jobService).getMyJobs();
	}
}
