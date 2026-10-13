package com.alumni.management.student.entity;

import com.alumni.management.user.entity.User;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "student_profiles")
public class StudentProfile {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	private User user;

	// Academic Details
	private String department;
	private String degree;
	private Integer batchYear;
	private String semester;

	// Skills & Interests
	private String skills;
	private String interests;
	private String bio;

	// Contact & Links
	private String linkedInUrl;
	private String githubUrl;
	private String contactNumber;
	private String currentCity;
	private String profilePictureUrl;

	public StudentProfile() {
	}

	public StudentProfile(Long id, User user, String department, String degree, Integer batchYear, String semester,
			String skills, String interests, String bio, String linkedInUrl, String githubUrl, String contactNumber,
			String currentCity, String profilePictureUrl) {
		this.id = id;
		this.user = user;
		this.department = department;
		this.degree = degree;
		this.batchYear = batchYear;
		this.semester = semester;
		this.skills = skills;
		this.interests = interests;
		this.bio = bio;
		this.linkedInUrl = linkedInUrl;
		this.githubUrl = githubUrl;
		this.contactNumber = contactNumber;
		this.currentCity = currentCity;
		this.profilePictureUrl = profilePictureUrl;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public String getDepartment() {
		return department;
	}

	public void setDepartment(String department) {
		this.department = department;
	}

	public String getDegree() {
		return degree;
	}

	public void setDegree(String degree) {
		this.degree = degree;
	}

	public Integer getBatchYear() {
		return batchYear;
	}

	public void setBatchYear(Integer batchYear) {
		this.batchYear = batchYear;
	}

	public String getSemester() {
		return semester;
	}

	public void setSemester(String semester) {
		this.semester = semester;
	}

	public String getSkills() {
		return skills;
	}

	public void setSkills(String skills) {
		this.skills = skills;
	}

	public String getInterests() {
		return interests;
	}

	public void setInterests(String interests) {
		this.interests = interests;
	}

	public String getBio() {
		return bio;
	}

	public void setBio(String bio) {
		this.bio = bio;
	}

	public String getLinkedInUrl() {
		return linkedInUrl;
	}

	public void setLinkedInUrl(String linkedInUrl) {
		this.linkedInUrl = linkedInUrl;
	}

	public String getGithubUrl() {
		return githubUrl;
	}

	public void setGithubUrl(String githubUrl) {
		this.githubUrl = githubUrl;
	}

	public String getContactNumber() {
		return contactNumber;
	}

	public void setContactNumber(String contactNumber) {
		this.contactNumber = contactNumber;
	}

	public String getCurrentCity() {
		return currentCity;
	}

	public void setCurrentCity(String currentCity) {
		this.currentCity = currentCity;
	}

	public String getProfilePictureUrl() {
		return profilePictureUrl;
	}

	public void setProfilePictureUrl(String profilePictureUrl) {
		this.profilePictureUrl = profilePictureUrl;
	}
}
