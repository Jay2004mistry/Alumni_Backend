package com.alumni.management.faculty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

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

import com.alumni.management.faculty.dto.FacultyProfileDto;
import com.alumni.management.faculty.entity.FacultyProfile;
import com.alumni.management.faculty.repository.FacultyRepository;
import com.alumni.management.faculty.service.FacultyService;
import com.alumni.management.role.entity.Role;
import com.alumni.management.user.entity.User;
import com.alumni.management.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class FacultyServiceTest {

	@Mock
	private FacultyRepository facultyRepository;

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private FacultyService facultyService;

	private User facultyUser;
	private Role facultyRole;
	private FacultyProfile facultyProfile;

	@BeforeEach
	void setUp() {
		facultyRole = new Role("FACULTY");
		facultyRole.setId(3L);

		facultyUser = new User(112L, "Prerna Agarwal", "vivekposhiya82@gmail.com", "encodedPass", facultyRole);
		facultyUser.setVerified(true);

		facultyProfile = new FacultyProfile();
		facultyProfile.setId(1L);
		facultyProfile.setUser(facultyUser);
		facultyProfile.setDepartment("MCA");
		facultyProfile.setEmail("vivekposhiya82@gmail.com");
		facultyProfile.setSpecialization("Big Data Technologies, Machine Learning");
		facultyProfile.setSkills("Big Data Technologies, Machine Learning");

		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(facultyUser.getEmail(), "encodedPass")
		);
	}

	@Test
	void testGetFacultyProfileReturnsCorrectFacultyDetails() {
		when(userRepository.findByEmail(facultyUser.getEmail())).thenReturn(Optional.of(facultyUser));
		when(facultyRepository.findByUserId(112L)).thenReturn(Optional.of(facultyProfile));

		FacultyProfileDto dto = facultyService.getFacultyProfile();

		assertNotNull(dto);
		assertEquals("Prerna Agarwal", dto.getUserName());
		assertEquals("vivekposhiya82@gmail.com", dto.getEmail());
		assertEquals("MCA", dto.getDepartment());
		assertEquals("Big Data Technologies, Machine Learning", dto.getSpecialization());
	}

	@Test
	void testGetAllFacultyProfiles() {
		when(facultyRepository.findAll()).thenReturn(List.of(facultyProfile));

		List<FacultyProfileDto> profiles = facultyService.getAllFacultyProfiles();

		assertNotNull(profiles);
		assertEquals(1, profiles.size());
		assertEquals("Prerna Agarwal", profiles.get(0).getUserName());
		assertEquals("vivekposhiya82@gmail.com", profiles.get(0).getEmail());
	}
}

