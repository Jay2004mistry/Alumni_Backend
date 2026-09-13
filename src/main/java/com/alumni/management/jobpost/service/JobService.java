package com.alumni.management.jobpost.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
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

		if (job.getUser() != null) {
			Long userId = job.getUser().getId();
			if (job.getUser().getRole() != null) {
				String roleName = job.getUser().getRole().getRoleName();
				if ("ALUMNI".equals(roleName)) {
					java.util.Optional<com.alumni.management.alumni.entity.AlumniProfile> alumniProfileOpt = alumniProfileRepository.findByUserId(userId);
					if (alumniProfileOpt.isPresent()) {
						com.alumni.management.alumni.entity.AlumniProfile profile = alumniProfileOpt.get();
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

		return new JobDto(job.getId(), job.getUser().getId(), job.getUser().getName(), job.getUser().getEmail(), job.getCompanyName(), job.getJobTitle(),
				job.getLocation(), job.getSalary(), job.getJobDescription(), job.getSkillsRequired(),
				job.getExperienceRequired(), job.getJoiningType(), job.getJobType(), job.getLastDateToApply(),
				job.getCompanyLink(), job.getCompanyEmail(), posterDepartment, posterBatchYear);
	}

	/**
	 * transactional Ensures "All or Nothing" database operations. If any part of
	 * this method fails or throws an Exception, Spring will automatically ROLLBACK
	 * (cancel) all database changes made during this process to maintain data
	 * integrity.
	 */
	private void validateJob(String title, String company, String location, String salary,
			String description, String skills, String experience, String joiningType,
			String jobType, java.time.LocalDate lastDate, String link, String email) {

		if (title == null || title.trim().isEmpty()) {
			throw new IllegalArgumentException("Job title is required.");
		}
		if (company == null || company.trim().isEmpty()) {
			throw new IllegalArgumentException("Company name is required.");
		}
		if (location == null || location.trim().isEmpty()) {
			throw new IllegalArgumentException("Job location is required.");
		}
		if (salary == null || salary.trim().isEmpty()) {
			throw new IllegalArgumentException("Annual salary in LPA is required.");
		}
		try {
			String clean = salary.trim().replaceAll("(?i)lpa", "").replaceAll("₹", "").trim();
			double num = Double.parseDouble(clean);
			if (num <= 0 || num > 200) {
				throw new IllegalArgumentException("Salary must be a positive annual package in LPA between 0.1 and 200.");
			}
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Please enter a valid numeric salary in LPA (e.g. 3.5, 6, 12).");
		}
		if (description == null || description.trim().isEmpty()) {
			throw new IllegalArgumentException("Job description is required.");
		}
		if (skills == null || skills.trim().isEmpty()) {
			throw new IllegalArgumentException("Skills required must not be empty.");
		}
		if (experience == null || experience.trim().isEmpty()) {
			throw new IllegalArgumentException("Experience required must not be empty.");
		}
		if (joiningType == null || joiningType.trim().isEmpty()) {
			throw new IllegalArgumentException("Joining type is required.");
		}
		if (jobType == null || jobType.trim().isEmpty()) {
			throw new IllegalArgumentException("Job type is required.");
		}
		if (lastDate == null) {
			throw new IllegalArgumentException("Application deadline is required.");
		}
		if (link == null || link.trim().isEmpty()) {
			throw new IllegalArgumentException("Company website / application link is required.");
		}
		if (email == null || email.trim().isEmpty() || !email.contains("@")) {
			throw new IllegalArgumentException("A valid company contact email is required.");
		}
	}

	private String normalizeLpaString(String rawSalary) {
		if (rawSalary == null || rawSalary.trim().isEmpty()) {
			return "";
		}
		String clean = rawSalary.trim();
		if (clean.toLowerCase().endsWith("lpa")) {
			return clean;
		}
		try {
			double num = Double.parseDouble(clean.replaceAll("₹", "").trim());
			String formattedNum = (num % 1 == 0) ? String.valueOf((int) num) : String.valueOf(num);
			return formattedNum + " LPA";
		} catch (Exception e) {
			return clean + " LPA";
		}
	}

	@Transactional
	public String createJobpost(Job job) {
		User user = getCurrUser();
		String userRole = user.getRole().getRoleName();
		if (!"ALUMNI".equals(userRole) && !"FACULTY".equals(userRole) && !"ADMIN".equals(userRole)) {
			throw new RuntimeException("Only alumni, faculty, and admin can create job posts");
		}

		validateJob(job.getJobTitle(), job.getCompanyName(), job.getLocation(), job.getSalary(),
				job.getJobDescription(), job.getSkillsRequired(), job.getExperienceRequired(),
				job.getJoiningType(), job.getJobType(), job.getLastDateToApply(),
				job.getCompanyLink(), job.getCompanyEmail());

		job.setSalary(normalizeLpaString(job.getSalary()));
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
		List<Job> jobs = jobRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
		return jobs.stream().map(this::convertToDto).collect(Collectors.toList());
	}

	public List<JobDto> getMyJobs() {
		User currentUser = getCurrUser();
		List<Job> jobs = jobRepository.findByUserId(currentUser.getId());
		return jobs.stream()
				.sorted((a, b) -> b.getId().compareTo(a.getId()))
				.map(this::convertToDto)
				.collect(Collectors.toList());
	}

	@Transactional
	public String updateJob(Long id, JobDto jobDto) {
		User currentUser = getCurrUser();

		Job existingJob = jobRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + id));

		String userRole = currentUser.getRole().getRoleName();
		if (!existingJob.getUser().getId().equals(currentUser.getId()) && !"ADMIN".equals(userRole)) {
			throw new RuntimeException("You are not authorized to update this job post");
		}

		validateJob(jobDto.getJobTitle(), jobDto.getCompanyName(), jobDto.getLocation(), jobDto.getSalary(),
				jobDto.getJobDescription(), jobDto.getSkillsRequired(), jobDto.getExperienceRequired(),
				jobDto.getJoiningType(), jobDto.getJobType(), jobDto.getLastDateToApply(),
				jobDto.getCompanyLink(), jobDto.getCompanyEmail());

		existingJob.setCompanyName(jobDto.getCompanyName());
		existingJob.setJobTitle(jobDto.getJobTitle());
		existingJob.setLocation(jobDto.getLocation());
		existingJob.setSalary(normalizeLpaString(jobDto.getSalary()));
		existingJob.setJobDescription(jobDto.getJobDescription());
		existingJob.setSkillsRequired(jobDto.getSkillsRequired());
		existingJob.setExperienceRequired(jobDto.getExperienceRequired());
		existingJob.setJoiningType(jobDto.getJoiningType());
		existingJob.setJobType(jobDto.getJobType());
		existingJob.setLastDateToApply(jobDto.getLastDateToApply());
		existingJob.setCompanyLink(jobDto.getCompanyLink());
		existingJob.setCompanyEmail(jobDto.getCompanyEmail());

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
