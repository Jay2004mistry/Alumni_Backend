package com.alumni.management.jobpost.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.alumni.management.exception.ResourceNotFoundException;
import com.alumni.management.jobpost.dto.JobDto;
import com.alumni.management.jobpost.entity.Job;
import com.alumni.management.jobpost.repository.JobRepository;
import com.alumni.management.user.entity.User;
import com.alumni.management.user.repository.UserRepository;

import jakarta.transaction.Transactional;
import com.alumni.management.notification.entity.Notification;
import com.alumni.management.notification.repository.NotificationRepository;

@Service
public class JobService {

	@Autowired
	JobRepository jobRepository;
	@Autowired
	UserRepository userRepository;
	@Autowired
	NotificationRepository notificationRepository;
	@Autowired
	com.alumni.management.alumni.repository.AlumniProfileRepository alumniProfileRepository;
	@Autowired
	com.alumni.management.faculty.repository.FacultyRepository facultyRepository;

//	jwt authentication
	private User getCurrUser() {
		String email = SecurityContextHolder.getContext().getAuthentication().getName();
		return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
	}

//	onvert he job entity into dto for use
	private JobDto convertToDto(Job job) {
		String posterDepartment = "N/A";
		String posterBatchYear = "N/A";
		String photoUrl = null;

		if (job.getUser() != null) {
			Long userId = job.getUser().getId();
			if (job.getUser().getRole() != null) {
				String roleName = job.getUser().getRole().getRoleName();
				if ("ALUMNI".equals(roleName)) {
					java.util.Optional<com.alumni.management.alumni.entity.AlumniProfile> alumniProfileOpt = alumniProfileRepository.findByUserId(userId);
					if (alumniProfileOpt.isPresent()) {
						com.alumni.management.alumni.entity.AlumniProfile profile = alumniProfileOpt.get();
						photoUrl = profile.getProfilePictureUrl();
						if (profile.getDepartment() != null && !profile.getDepartment().isEmpty()) {
							posterDepartment = profile.getDepartment();
						}
						if (profile.getBatchYear() != null) {
							posterBatchYear = "Class of " + profile.getBatchYear().toString();
						}
					}
				} else if ("FACULTY".equals(roleName)) {
					java.util.Optional<com.alumni.management.faculty.entity.FacultyProfile> facultyProfileOpt = facultyRepository.findByUserId(userId);
					if (facultyProfileOpt.isPresent()) {
						com.alumni.management.faculty.entity.FacultyProfile profile = facultyProfileOpt.get();
						photoUrl = profile.getProfilePictureUrl();
						if (profile.getDepartment() != null && !profile.getDepartment().isEmpty()) {
							posterDepartment = profile.getDepartment();
						}
						if (profile.getDesignation() != null && !profile.getDesignation().isEmpty()) {
							posterBatchYear = profile.getDesignation();
						} else {
							posterBatchYear = "Faculty";
						}
					}
				} else if ("ADMIN".equals(roleName)) {
					posterDepartment = "Administration";
					posterBatchYear = "Staff";
				}
			}
		}

		JobDto dto = new JobDto(job.getId(), job.getUser().getId(), job.getUser().getName(), job.getUser().getEmail(), job.getCompanyName(), job.getJobTitle(),
				job.getLocation(), job.getSalary(), job.getJobDescription(), job.getSkillsRequired(),
				job.getExperienceRequired(), job.getJoiningType(), job.getJobType(), job.getLastDateToApply(),
				job.getCompanyLink(), job.getCompanyEmail(), posterDepartment, posterBatchYear);
		dto.setPhotoUrl(photoUrl);
		return dto;
	}

	/**
	 * transactional Ensures "All or Nothing" database operations. If any part of
	 * this method fails or throws an Exception, Spring will automatically ROLLBACK
	 * (cancel) all database changes made during this process to maintain data
	 * integrity.
	 */
	@Transactional
	public String createJobpost(Job job) {
		User user = getCurrUser();
		// Check if user has permission to post jobs (Alumni, Faculty, or Admin)
		String userRole = user.getRole().getRoleName();
		if (!"ALUMNI".equals(userRole) && !"FACULTY".equals(userRole) && !"ADMIN".equals(userRole)) {
			throw new RuntimeException("Only alumni, faculty, and admin can create job posts");
		}
//Set the user to the job post
		job.setId(null);
		job.setUser(user);
		jobRepository.save(job);

		try {
			Notification notification = new Notification();
			notification.setTitle("New Job Posted");
			notification.setMessage(job.getJobTitle() + " role at " + job.getCompanyName() + " posted by " + user.getName());
			notification.setTimestamp(java.time.LocalDateTime.now());
			notification.setRead(false);
			notificationRepository.save(notification);
		} catch (Exception e) {
			// Silently fail notification so it doesn't block main flow
		}

		return "Job post created successfully by " + user.getName();
	}

	public List<JobDto> getAllJobs() {
		List<Job> jobs = jobRepository.findAll();
		return jobs.stream().map(this::convertToDto).collect(Collectors.toList());
	}

	public List<JobDto> getMyJobs() {
		User user = getCurrUser();
		List<Job> jobs = jobRepository.findByUserId(user.getId());
		return jobs.stream().map(this::convertToDto).collect(Collectors.toList());
	}

	@Transactional
	public String updateJob(Long id, JobDto jobDto) {
		User currentUser = getCurrUser();

		Job existingJob = jobRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + id));

		// Check if current user is the owner of the job or an admin
		String userRole = (currentUser.getRole() != null && currentUser.getRole().getRoleName() != null)
				? currentUser.getRole().getRoleName().toUpperCase()
				: "";
		boolean isOwner = existingJob.getUser() != null && existingJob.getUser().getId().equals(currentUser.getId());
		boolean isAdmin = "ADMIN".equals(userRole);

		if (!isOwner && !isAdmin) {
			throw new SecurityException("You are not authorized to update this job post");
		}

		// Validation of required fields
		if (jobDto.getCompanyName() == null || jobDto.getCompanyName().trim().isEmpty()) {
			throw new IllegalArgumentException("Company name is required");
		}
		if (jobDto.getJobTitle() == null || jobDto.getJobTitle().trim().length() < 2) {
			throw new IllegalArgumentException("Job title must be at least 2 characters");
		}
		if (jobDto.getLocation() == null || jobDto.getLocation().trim().isEmpty()) {
			throw new IllegalArgumentException("Location is required");
		}
		if (jobDto.getSalary() == null || jobDto.getSalary().trim().isEmpty()) {
			throw new IllegalArgumentException("Salary is required");
		}
		if (jobDto.getJobDescription() == null || jobDto.getJobDescription().trim().length() < 10) {
			throw new IllegalArgumentException("Job description must be at least 10 characters");
		}
		if (jobDto.getSkillsRequired() == null || jobDto.getSkillsRequired().trim().isEmpty()) {
			throw new IllegalArgumentException("Skills required cannot be empty");
		}
		if (jobDto.getExperienceRequired() == null || jobDto.getExperienceRequired().trim().isEmpty()) {
			throw new IllegalArgumentException("Experience required cannot be empty");
		}
		if (jobDto.getLastDateToApply() == null) {
			throw new IllegalArgumentException("Last date to apply is required");
		}

		// Update job details (preserve the original owner user)
		existingJob.setCompanyName(jobDto.getCompanyName().trim());
		existingJob.setJobTitle(jobDto.getJobTitle().trim());
		existingJob.setLocation(jobDto.getLocation().trim());
		existingJob.setSalary(jobDto.getSalary().trim());
		existingJob.setJobDescription(jobDto.getJobDescription().trim());
		existingJob.setSkillsRequired(jobDto.getSkillsRequired().trim());
		existingJob.setExperienceRequired(jobDto.getExperienceRequired().trim());
		existingJob.setJoiningType(jobDto.getJoiningType() != null && !jobDto.getJoiningType().trim().isEmpty() ? jobDto.getJoiningType().trim() : "Immediate");
		existingJob.setJobType(jobDto.getJobType() != null && !jobDto.getJobType().trim().isEmpty() ? jobDto.getJobType().trim() : "Full Time");
		existingJob.setLastDateToApply(jobDto.getLastDateToApply());
		existingJob.setCompanyLink(jobDto.getCompanyLink() != null ? jobDto.getCompanyLink().trim() : null);
		existingJob.setCompanyEmail(jobDto.getCompanyEmail() != null ? jobDto.getCompanyEmail().trim() : null);

		jobRepository.save(existingJob);
		return "Job post updated successfully with ID: " + id;
	}

	@Transactional
	public String deleteJob(Long id) {
		User currentUser = getCurrUser();

		Job job = jobRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + id));

		// Check if current user is the owner of the job or admin
		String userRole = currentUser.getRole().getRoleName();
		if (!job.getUser().getId().equals(currentUser.getId()) && !"ADMIN".equals(userRole)) {
			throw new RuntimeException("You are not authorized to delete this job post");
		}

		jobRepository.delete(job);
		return "Job post deleted successfully with ID: " + id;
	}

}
