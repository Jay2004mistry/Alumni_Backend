package com.alumni.management.student.dto;

public class StudentProfileDto {

	private Long userId;
	private String userName;
	private String email;
	private String universityNumber;
	private String department;
	private String degree;
	private Integer batchYear;
	private String semester;
	private String skills;
	private String interests;
	private String bio;
	private String linkedInUrl;
	private String githubUrl;
	private String contactNumber;
	private String currentCity;
	private String profilePictureUrl;

	public StudentProfileDto() {
	}

	public StudentProfileDto(Long userId, String userName, String email, String universityNumber, String department,
			String degree, Integer batchYear, String semester, String skills, String interests, String bio,
			String linkedInUrl, String githubUrl, String contactNumber, String currentCity, String profilePictureUrl) {
		this.userId = userId;
		this.userName = userName;
		this.email = email;
		this.universityNumber = universityNumber;
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

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getUniversityNumber() {
		return universityNumber;
	}

	public void setUniversityNumber(String universityNumber) {
		this.universityNumber = universityNumber;
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
