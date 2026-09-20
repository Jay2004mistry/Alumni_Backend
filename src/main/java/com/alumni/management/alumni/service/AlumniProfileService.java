package com.alumni.management.alumni.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.alumni.management.alumni.dto.AlumniProfileDto;
import com.alumni.management.alumni.entity.AlumniProfile;
import com.alumni.management.alumni.repository.AlumniProfileRepository;
import com.alumni.management.exception.ResourceNotFoundException;
import com.alumni.management.user.entity.User;
import com.alumni.management.user.repository.UserRepository;

@Service
public class AlumniProfileService {

	@Autowired
	AlumniProfileRepository profileRepository;

	@Autowired
	UserRepository UserRepository;

//	Jwt Authentication
	private User getCurrentUser() {
		String email = SecurityContextHolder.getContext().getAuthentication().getName();
		return UserRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
	}

//	Help to not write this everytime just call it using map(this::converToDto)
	private AlumniProfileDto convertToDto(AlumniProfile alumniProfile) {
		AlumniProfileDto dto = new AlumniProfileDto(alumniProfile.getUser().getId(),
				alumniProfile.getUser().getName(), alumniProfile.getBatchYear(), alumniProfile.getDegree(),
				alumniProfile.getDepartment(), alumniProfile.getDesignation(), alumniProfile.getCompanyName(),
				alumniProfile.getIndustry(), alumniProfile.getSkills(), alumniProfile.getWorkExperience(),
				alumniProfile.getLinkedInUrl(), alumniProfile.getGithubUrl(), alumniProfile.getContactNumber(),
				alumniProfile.getCurrentCity());
		dto.setProfilePictureUrl(alumniProfile.getProfilePictureUrl());
		dto.setEmail(alumniProfile.getUser().getEmail());
		return dto;
	}

	public String createProfile(AlumniProfile profile) {

		User user = getCurrentUser();
		if (!"ALUMNI".equals(user.getRole().getRoleName())) {
			throw new RuntimeException("Only alumni can create the profile");
		}

		AlumniProfile alumniProfile = profileRepository.findByUserId(user.getId()).orElse(new AlumniProfile());

		alumniProfile.setUser(user);
		alumniProfile.setBatchYear(profile.getBatchYear());
		alumniProfile.setDegree(profile.getDegree());
		alumniProfile.setDepartment(profile.getDepartment());

		alumniProfile.setDesignation(profile.getDesignation());
		alumniProfile.setCompanyName(profile.getCompanyName());
		alumniProfile.setIndustry(profile.getIndustry());
		alumniProfile.setSkills(profile.getSkills() != null ? profile.getSkills().toLowerCase().trim() : "");
		alumniProfile.setWorkExperience(profile.getWorkExperience());

		alumniProfile.setLinkedInUrl(profile.getLinkedInUrl());
		alumniProfile.setGithubUrl(profile.getGithubUrl());
		alumniProfile.setContactNumber(profile.getContactNumber());
		alumniProfile.setCurrentCity(profile.getCurrentCity());
		alumniProfile.setProfilePictureUrl(profile.getProfilePictureUrl());

		profileRepository.save(alumniProfile);

		return "Profile saved successfully";
	}

	public AlumniProfileDto getProfileByUserId() {
		User user = getCurrentUser();
		AlumniProfile alumniProfile = profileRepository.findByUserId(user.getId())
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));
		return convertToDto(alumniProfile); // This one line does everything!
	}

	public String updateProfile(AlumniProfile profile) {
		User user = getCurrentUser();

		AlumniProfile alumniProfile = profileRepository.findByUserId(user.getId()).orElse(new AlumniProfile());
//		if user edit and then login save data gone so i have to add this line so data keep 
//		even after logout and then login
		alumniProfile.setUser(user);
		alumniProfile.setBatchYear(profile.getBatchYear());
		alumniProfile.setDegree(profile.getDegree());
		alumniProfile.setDepartment(profile.getDepartment());

		alumniProfile.setDesignation(profile.getDesignation());
		alumniProfile.setCompanyName(profile.getCompanyName());
		alumniProfile.setIndustry(profile.getIndustry());
		alumniProfile.setSkills(profile.getSkills());
		alumniProfile.setWorkExperience(profile.getWorkExperience());

		alumniProfile.setLinkedInUrl(profile.getLinkedInUrl());
		alumniProfile.setGithubUrl(profile.getGithubUrl());
		alumniProfile.setContactNumber(profile.getContactNumber());
		alumniProfile.setCurrentCity(profile.getCurrentCity());
		alumniProfile.setProfilePictureUrl(profile.getProfilePictureUrl());

		profileRepository.save(alumniProfile);

		return "Profile update successfully";

	}

	public String deleteProfile() {
		User user = getCurrentUser();

		AlumniProfile alumniProfile = profileRepository.findByUserId(user.getId())
				.orElseThrow(() -> new ResourceNotFoundException("user not found"));
		profileRepository.delete(alumniProfile);

		return "Delete the user profile successfully";
	}

	public List<AlumniProfileDto> searchByName(String name) {
		List<AlumniProfile> alumniProfiles = profileRepository.findByName(name);
		return alumniProfiles.stream().map(this::convertToDto).collect(Collectors.toList());
	}

	public List<AlumniProfileDto> searchByBatchYear(Integer batchYear) {
		List<AlumniProfile> alumniProfile = profileRepository.findByBatchYear(batchYear);

		return alumniProfile.stream().map(this::convertToDto).collect(Collectors.toList());
	}

	public List<AlumniProfileDto> searchByDepartment(String department) {
		List<AlumniProfile> alumniProfile = profileRepository.findByDepartment(department);
		return alumniProfile.stream().map(this::convertToDto).collect(Collectors.toList());
	}

//	open profile based on click on name
	// 1. Get all alumni profiles
	public List<AlumniProfileDto> getAllAlumniProfiles() {
		List<AlumniProfile> profiles = profileRepository.findAll();
		return profiles.stream().map(this::convertToDto).collect(Collectors.toList());
	}

	// 2. Get profile by user ID (you already have this method but let's add it)
	public AlumniProfileDto getAlumniProfileByUserId(Long userId) {
		AlumniProfile profile = profileRepository.findByUserId(userId)
				.orElseThrow(() -> new ResourceNotFoundException("Profile not found for user id: " + userId));
		return convertToDto(profile);
	}

}
