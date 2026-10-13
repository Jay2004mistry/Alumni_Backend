package com.alumni.management.student.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alumni.management.student.dto.StudentProfileDto;
import com.alumni.management.student.entity.StudentProfile;
import com.alumni.management.student.service.StudentProfileService;

@RestController
@RequestMapping("/api/student")
public class StudentProfileController {

	@Autowired
	private StudentProfileService studentProfileService;

	@GetMapping("/profile")
	public StudentProfileDto getProfile() {
		return studentProfileService.getProfile();
	}

	@PreAuthorize("hasRole('STUDENT')")
	@PostMapping("/profile")
	public String createProfile(@RequestBody StudentProfile profile) {
		return studentProfileService.createOrUpdateProfile(profile);
	}

	@PreAuthorize("hasRole('STUDENT')")
	@PutMapping("/profile")
	public String updateProfile(@RequestBody StudentProfile profile) {
		return studentProfileService.createOrUpdateProfile(profile);
	}

	@GetMapping("/user/{userId}")
	public StudentProfileDto getProfileByUserId(@PathVariable Long userId) {
		return studentProfileService.getProfileByUserId(userId);
	}

	@GetMapping
	public List<StudentProfileDto> getAllStudentProfiles() {
		return studentProfileService.getAllStudentProfiles();
	}
}
