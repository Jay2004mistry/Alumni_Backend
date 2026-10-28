package com.alumni.management;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.alumni.management.alumni.entity.AlumniProfile;
import com.alumni.management.alumni.repository.AlumniProfileRepository;
import com.alumni.management.faculty.entity.FacultyProfile;
import com.alumni.management.faculty.repository.FacultyRepository;
import com.alumni.management.jobpost.dto.JobDto;
import com.alumni.management.jobpost.entity.Job;
import com.alumni.management.jobpost.repository.JobRepository;
import com.alumni.management.jobpost.service.JobService;
import com.alumni.management.role.entity.Role;
import com.alumni.management.user.entity.User;
import com.alumni.management.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class JobsAndEventsDatasetTest {

	@Mock
	private JobRepository jobRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private AlumniProfileRepository alumniProfileRepository;

	@Mock
	private FacultyRepository facultyRepository;

	@InjectMocks
	private JobService jobService;

	private User facultyUser;
	private User alumniUser;
	private Job facultyJob;
	private Job alumniJob;

	@BeforeEach
	void setUp() {
		Role facultyRole = new Role("FACULTY");
		facultyRole.setId(3L);
		facultyUser = new User(137L, "Arpit Jain", "arpitjain34@gmail.com", "pass", facultyRole);

		FacultyProfile fp = new FacultyProfile();
		fp.setUser(facultyUser);
		fp.setDepartment("MCA");
		fp.setDesignation("Assistant Professor");

		Role alumniRole = new Role("ALUMNI");
		alumniRole.setId(1L);
		alumniUser = new User(120L, "Vivek Poshiya", "202500719010175@glsu.edu.in", "pass", alumniRole);

		AlumniProfile ap = new AlumniProfile();
		ap.setUser(alumniUser);
		ap.setDepartment("MCA");
		ap.setBatchYear(2025);

		facultyJob = new Job();
		facultyJob.setId(47L);
		facultyJob.setUser(facultyUser);
		facultyJob.setJobTitle("Junior Java Developer");
		facultyJob.setCompanyName("Persistent Systems");
		facultyJob.setLocation("Ahmedabad");
		facultyJob.setSalary("₹4.5 LPA");
		facultyJob.setJobDescription("[Demo Listing] Entry-level Java Developer role");
		facultyJob.setSkillsRequired("Java, Spring Boot, MySQL");
		facultyJob.setExperienceRequired("0-1 Years");
		facultyJob.setJoiningType("Immediate");
		facultyJob.setJobType("Full Time");
		facultyJob.setLastDateToApply(LocalDate.of(2026, 12, 15));
		facultyJob.setCompanyEmail("careers.demo@persistent.com");
		facultyJob.setCompanyLink("https://example.com/careers/demo/java-dev");

		alumniJob = new Job();
		alumniJob.setId(48L);
		alumniJob.setUser(alumniUser);
		alumniJob.setJobTitle("Python Developer");
		alumniJob.setCompanyName("Crest Data Systems");
		alumniJob.setLocation("Ahmedabad");
		alumniJob.setSalary("₹5.5 LPA");
		alumniJob.setJobDescription("[Demo Listing] Seeking Python Developer");
		alumniJob.setSkillsRequired("Python, Django, FastAPI");
		alumniJob.setExperienceRequired("1-2 Years");
		alumniJob.setJoiningType("Immediate");
		alumniJob.setJobType("Full Time");
		alumniJob.setLastDateToApply(LocalDate.of(2026, 12, 20));
		alumniJob.setCompanyEmail("jobs.demo@crestdata.com");
		alumniJob.setCompanyLink("https://example.com/careers/demo/python-dev");
	}

	@Test
	void testGetAllJobs_MapsFacultyAndAlumniOrganizersCorrectly() {
		when(jobRepository.findAll()).thenReturn(List.of(facultyJob, alumniJob));
		when(facultyRepository.findByUserId(137L)).thenReturn(Optional.of(new FacultyProfile() {{
			setDepartment("MCA");
			setDesignation("Assistant Professor");
		}}));
		when(alumniProfileRepository.findByUserId(120L)).thenReturn(Optional.of(new AlumniProfile() {{
			setDepartment("MCA");
			setBatchYear(2025);
		}}));

		List<JobDto> jobs = jobService.getAllJobs();

		assertNotNull(jobs);
		assertEquals(2, jobs.size());

		// Faculty job assertions
		JobDto fj = jobs.get(0);
		assertEquals("Junior Java Developer", fj.getJobTitle());
		assertEquals("Persistent Systems", fj.getCompanyName());
		assertEquals("Arpit Jain", fj.getUserName());
		assertEquals("arpitjain34@gmail.com", fj.getUserEmail());
		assertEquals("MCA", fj.getPosterDepartment());
		assertEquals("Assistant Professor", fj.getPosterBatchYear());

		// Alumni job assertions
		JobDto aj = jobs.get(1);
		assertEquals("Python Developer", aj.getJobTitle());
		assertEquals("Crest Data Systems", aj.getCompanyName());
		assertEquals("Vivek Poshiya", aj.getUserName());
		assertEquals("202500719010175@glsu.edu.in", aj.getUserEmail());
		assertEquals("MCA", aj.getPosterDepartment());
		assertEquals("Class of 2025", aj.getPosterBatchYear());
	}
}

