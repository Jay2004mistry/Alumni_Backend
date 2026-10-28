package com.alumni.management.faculty.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.alumni.management.exception.ResourceNotFoundException;
import com.alumni.management.faculty.dto.FacultyProfileDto;
import com.alumni.management.faculty.entity.FacultyProfile;
import com.alumni.management.faculty.repository.FacultyRepository;
import com.alumni.management.user.entity.User;
import com.alumni.management.user.repository.UserRepository;

@Service
public class FacultyService {

	@Autowired
	FacultyRepository facultyRepository;

	@Autowired
	UserRepository userRepository;

	private User getCurrentUser() {
		String email = SecurityContextHolder.getContext().getAuthentication().getName();
		return userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
	}

	public String createFacultyProfile(FacultyProfile facultyProfile) {
		User user = getCurrentUser();
		if (!"FACULTY".equals(user.getRole().getRoleName())) {
			throw new RuntimeException("Only faculty can create profile");
		}
		FacultyProfile profile = facultyRepository.findByUserId(user.getId()).orElse(new FacultyProfile());
		profile.setUser(user);
		profile.setDepartment(facultyProfile.getDepartment());
		profile.setDesignation(facultyProfile.getDesignation());
		profile.setQualification(facultyProfile.getQualification());
		profile.setSpecialization(facultyProfile.getSpecialization());
		profile.setExperienceYears(facultyProfile.getExperienceYears());

		profile.setEmail(facultyProfile.getEmail());
		profile.setContactNumber(facultyProfile.getContactNumber());

		profile.setResearchInterests(facultyProfile.getResearchInterests());
		profile.setBio(facultyProfile.getBio());
		profile.setLinkedInUrl(facultyProfile.getLinkedInUrl());

		// New fields mapping
		profile.setTeachingExperience(facultyProfile.getTeachingExperience());
		profile.setIndustryExperience(facultyProfile.getIndustryExperience());
		profile.setPublicationsCount(facultyProfile.getPublicationsCount());
		profile.setCertifications(facultyProfile.getCertifications());
		profile.setAchievements(facultyProfile.getAchievements());
		profile.setSkills(facultyProfile.getSkills());
		profile.setStudentsGuided(facultyProfile.getStudentsGuided());
		profile.setProjectsSupervised(facultyProfile.getProjectsSupervised());
		profile.setProfilePictureUrl(facultyProfile.getProfilePictureUrl());

		facultyRepository.save(profile);
		return "Profile add successfully";
	}

	private FacultyProfileDto convertToDto(FacultyProfile profile) {
		FacultyProfileDto dto = new FacultyProfileDto(
				profile.getUser().getId(),
				profile.getUser().getName(),
				profile.getDepartment() != null ? profile.getDepartment() : "MCA", 
				profile.getDesignation() != null ? profile.getDesignation() : "Faculty Member", 
				profile.getQualification() != null ? profile.getQualification() : "MCA / Ph.D.",
				profile.getSpecialization(), 
				profile.getExperienceYears(), 
				profile.getEmail() != null ? profile.getEmail() : profile.getUser().getEmail(),
				profile.getContactNumber(), 
				profile.getResearchInterests(), 
				profile.getBio(), 
				profile.getLinkedInUrl(),
				profile.getTeachingExperience(),
				profile.getIndustryExperience(),
				profile.getPublicationsCount(),
				profile.getCertifications(),
				profile.getAchievements(),
				profile.getSkills(),
				profile.getStudentsGuided(),
				profile.getProjectsSupervised()
		);
		dto.setProfilePictureUrl(profile.getProfilePictureUrl());
		return dto;
	}

	public FacultyProfileDto getFacultyProfile() {
		User user = getCurrentUser();
		FacultyProfile profile = facultyRepository.findByUserId(user.getId())
				.orElseGet(() -> {
					FacultyProfile fp = new FacultyProfile();
					fp.setUser(user);
					fp.setDepartment("MCA");
					fp.setDesignation("Faculty Member");
					fp.setEmail(user.getEmail());
					return facultyRepository.save(fp);
				});

		return convertToDto(profile);
	}

	public List<FacultyProfileDto> getAllFacultyProfiles() {
		return facultyRepository.findAll().stream().map(this::convertToDto).collect(Collectors.toList());
	}

	public FacultyProfileDto getFacultyProfileByUserId(Long userId) {
		FacultyProfile profile = facultyRepository.findByUserId(userId)
				.orElseGet(() -> {
					User user = userRepository.findById(userId)
							.orElseThrow(() -> new ResourceNotFoundException("Faculty profile not found for user: " + userId));
					FacultyProfile fp = new FacultyProfile();
					fp.setUser(user);
					fp.setDepartment("MCA");
					fp.setDesignation("Faculty Member");
					fp.setEmail(user.getEmail());
					return facultyRepository.save(fp);
				});
		return convertToDto(profile);
	}

	public String updateFacultyProfile(FacultyProfile facultyProfile) {
		User user = getCurrentUser();

		FacultyProfile profile = facultyRepository.findByUserId(user.getId())
				.orElse(new FacultyProfile());
		profile.setUser(user);
		profile.setDepartment(facultyProfile.getDepartment());
		profile.setDesignation(facultyProfile.getDesignation());
		profile.setQualification(facultyProfile.getQualification());
		profile.setSpecialization(facultyProfile.getSpecialization());
		profile.setExperienceYears(facultyProfile.getExperienceYears());

		profile.setEmail(facultyProfile.getEmail());
		profile.setContactNumber(facultyProfile.getContactNumber());

		profile.setResearchInterests(facultyProfile.getResearchInterests());
		profile.setBio(facultyProfile.getBio());
		profile.setLinkedInUrl(facultyProfile.getLinkedInUrl());

		// New fields mapping
		profile.setTeachingExperience(facultyProfile.getTeachingExperience());
		profile.setIndustryExperience(facultyProfile.getIndustryExperience());
		profile.setPublicationsCount(facultyProfile.getPublicationsCount());
		profile.setCertifications(facultyProfile.getCertifications());
		profile.setAchievements(facultyProfile.getAchievements());
		profile.setSkills(facultyProfile.getSkills());
		profile.setStudentsGuided(facultyProfile.getStudentsGuided());
		profile.setProjectsSupervised(facultyProfile.getProjectsSupervised());
		profile.setProfilePictureUrl(facultyProfile.getProfilePictureUrl());

		facultyRepository.save(profile);
		return "Profile Update successfully";
	}

	public String deleteFacultyProfile() {
		User user = getCurrentUser();

		FacultyProfile facultyProfile = facultyRepository.findByUserId(user.getId())
				.orElseThrow(() -> new ResourceNotFoundException("No faculty profile has been found"));
		facultyRepository.delete(facultyProfile);
		return "Faculty profile delete successfully";
	}

}
