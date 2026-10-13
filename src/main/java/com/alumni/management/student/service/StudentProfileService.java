package com.alumni.management.student.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alumni.management.exception.ResourceNotFoundException;
import com.alumni.management.student.dto.StudentProfileDto;
import com.alumni.management.student.entity.StudentProfile;
import com.alumni.management.student.repository.StudentProfileRepository;
import com.alumni.management.user.entity.User;
import com.alumni.management.user.repository.UserRepository;

@Service
public class StudentProfileService {

	@Autowired
	private StudentProfileRepository profileRepository;

	@Autowired
	private UserRepository userRepository;

	private User getCurrentUser() {
		String email = SecurityContextHolder.getContext().getAuthentication().getName();
		return userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
	}

	private StudentProfileDto convertToDto(StudentProfile p) {
		return new StudentProfileDto(
				p.getUser().getId(),
				p.getUser().getName(),
				p.getUser().getEmail(),
				p.getUser().getUniversityNumber(),
				p.getDepartment() != null ? p.getDepartment() : "MCA",
				p.getDegree() != null ? p.getDegree() : "Master of Computer Applications",
				p.getBatchYear(),
				p.getSemester() != null ? p.getSemester() : "Semester 4",
				p.getSkills(),
				p.getInterests(),
				p.getBio(),
				p.getLinkedInUrl(),
				p.getGithubUrl(),
				p.getContactNumber(),
				p.getCurrentCity(),
				p.getProfilePictureUrl()
		);
	}

	public StudentProfileDto getProfile() {
		User user = getCurrentUser();
		Optional<StudentProfile> opt = profileRepository.findByUserId(user.getId());
		if (opt.isPresent()) {
			return convertToDto(opt.get());
		}
		// Return initialized profile with user details if not yet saved
		return new StudentProfileDto(
				user.getId(),
				user.getName(),
				user.getEmail(),
				user.getUniversityNumber(),
				"MCA",
				"Master of Computer Applications",
				null,
				"Semester 4",
				"",
				"",
				"",
				"",
				"",
				"",
				"",
				null
		);
	}

	@Transactional
	public String createOrUpdateProfile(StudentProfile profile) {
		User user = getCurrentUser();
		String roleName = user.getRole() != null ? user.getRole().getRoleName().toUpperCase() : "";
		if (!"STUDENT".equals(roleName) && !"ADMIN".equals(roleName)) {
			throw new RuntimeException("Only students can update a student profile");
		}

		StudentProfile studentProfile = profileRepository.findByUserId(user.getId())
				.orElse(new StudentProfile());

		studentProfile.setUser(user);
		studentProfile.setDepartment(profile.getDepartment() != null ? profile.getDepartment() : "MCA");
		studentProfile.setDegree(profile.getDegree());
		studentProfile.setBatchYear(profile.getBatchYear());
		studentProfile.setSemester(profile.getSemester());
		studentProfile.setSkills(profile.getSkills() != null ? profile.getSkills().trim() : "");
		studentProfile.setInterests(profile.getInterests());
		studentProfile.setBio(profile.getBio());
		studentProfile.setLinkedInUrl(profile.getLinkedInUrl());
		studentProfile.setGithubUrl(profile.getGithubUrl());
		studentProfile.setContactNumber(profile.getContactNumber());
		studentProfile.setCurrentCity(profile.getCurrentCity());
		if (profile.getProfilePictureUrl() != null && !profile.getProfilePictureUrl().isEmpty()) {
			studentProfile.setProfilePictureUrl(profile.getProfilePictureUrl());
		}

		profileRepository.save(studentProfile);
		return "Profile saved successfully";
	}

	public StudentProfileDto getProfileByUserId(Long userId) {
		StudentProfile profile = profileRepository.findByUserId(userId)
				.orElseThrow(() -> new ResourceNotFoundException("Student profile not found for user id " + userId));
		return convertToDto(profile);
	}

	public List<StudentProfileDto> getAllStudentProfiles() {
		return profileRepository.findAll().stream()
				.map(this::convertToDto)
				.collect(Collectors.toList());
	}
}
