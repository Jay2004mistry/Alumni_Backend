package com.alumni.management.alumni;

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

import com.alumni.management.alumni.dto.AlumniProfileDto;
import com.alumni.management.alumni.entity.AlumniProfile;
import com.alumni.management.alumni.repository.AlumniProfileRepository;
import com.alumni.management.alumni.service.AlumniProfileService;
import com.alumni.management.role.entity.Role;
import com.alumni.management.user.entity.User;
import com.alumni.management.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class AlumniProfileServiceTest {

	@Mock
	private AlumniProfileRepository profileRepository;

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private AlumniProfileService profileService;

	private User alumniUser;
	private Role alumniRole;
	private AlumniProfile alumniProfile;

	@BeforeEach
	void setUp() {
		alumniRole = new Role("ALUMNI");
		alumniRole.setId(1L);

		alumniUser = new User(120L, "Vivek Poshiya", "202500719010175@glsu.edu.in", "encodedPass", alumniRole);
		alumniUser.setVerified(true);

		alumniProfile = new AlumniProfile();
		alumniProfile.setId(1L);
		alumniProfile.setUser(alumniUser);
		alumniProfile.setDegree("MCA");
		alumniProfile.setBatchYear(2025);
		alumniProfile.setDepartment("MCA");

		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(alumniUser.getEmail(), "encodedPass")
		);
	}

	@Test
	void testGetProfileByUserIdReturnsCorrectAcademicDetails() {
		when(userRepository.findByEmail(alumniUser.getEmail())).thenReturn(Optional.of(alumniUser));
		when(profileRepository.findByUserId(120L)).thenReturn(Optional.of(alumniProfile));

		AlumniProfileDto dto = profileService.getProfileByUserId();

		assertNotNull(dto);
		assertEquals("Vivek Poshiya", dto.getUserName());
		assertEquals("202500719010175@glsu.edu.in", dto.getEmail());
		assertEquals("MCA", dto.getDegree());
		assertEquals(2025, dto.getBatchYear());
		assertEquals("MCA", dto.getDepartment());
	}

	@Test
	void testGetAllAlumniProfiles() {
		when(profileRepository.findAll()).thenReturn(List.of(alumniProfile));

		List<AlumniProfileDto> profiles = profileService.getAllAlumniProfiles();

		assertNotNull(profiles);
		assertEquals(1, profiles.size());
		assertEquals("MCA", profiles.get(0).getDegree());
		assertEquals(2025, profiles.get(0).getBatchYear());
	}
}

