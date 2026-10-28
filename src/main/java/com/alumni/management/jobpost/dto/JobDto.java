package com.alumni.management.jobpost.dto;

import java.time.LocalDate;

public class JobDto {
	
	private Long id;
	private long userId;
	private String userName;
	private String userEmail;
	private String companyName;
	private String jobTitle;
	private String location;
	private String salary;
	private String jobDescription;

	private String skillsRequired;
	private String experienceRequired;
	private String joiningType;
	private String jobType;

	private LocalDate lastDateToApply;
	private String companyLink;
	private String companyEmail;
	private String posterDepartment;
	private String posterBatchYear;
	private String photoUrl;

	public JobDto(Long id, long userId, String userName, String userEmail, String companyName, String jobTitle, String location, String salary,
			String jobDescription, String skillsRequired, String experienceRequired, String joiningType, String jobType,
			LocalDate lastDateToApply, String companyLink, String companyEmail, String posterDepartment, String posterBatchYear) {
		super();
		this.id = id;
		this.userId = userId;
		this.userName = userName;
		this.userEmail = userEmail;
		this.companyName = companyName;
		this.jobTitle = jobTitle;
		this.location = location;
		this.salary = salary;
		this.jobDescription = jobDescription;
		this.skillsRequired = skillsRequired;
		this.experienceRequired = experienceRequired;
		this.joiningType = joiningType;
		this.jobType = jobType;
		this.lastDateToApply = lastDateToApply;
		this.companyLink = companyLink;
		this.companyEmail = companyEmail;
		this.posterDepartment = posterDepartment;
		this.posterBatchYear = posterBatchYear;
	}

	public JobDto(long userId, String userName, String userEmail, String companyName, String jobTitle, String location, String salary,
			String jobDescription, String skillsRequired, String experienceRequired, String joiningType, String jobType,
			LocalDate lastDateToApply, String companyLink, String companyEmail, String posterDepartment, String posterBatchYear) {
		this(null, userId, userName, userEmail, companyName, jobTitle, location, salary, jobDescription, skillsRequired, experienceRequired, joiningType, jobType, lastDateToApply, companyLink, companyEmail, posterDepartment, posterBatchYear);
	}

	public JobDto() {
		super();
	}

	public long getUserId() {
		return userId;
	}

	public void setUserId(long userId) {
		this.userId = userId;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getUserEmail() {
		return userEmail;
	}

	public void setUserEmail(String userEmail) {
		this.userEmail = userEmail;
	}

	public String getCompanyName() {
		return companyName;
	}

	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}

	public String getJobTitle() {
		return jobTitle;
	}

	public void setJobTitle(String jobTitle) {
		this.jobTitle = jobTitle;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getSalary() {
		return salary;
	}

	public void setSalary(String salary) {
		this.salary = salary;
	}

	public String getJobDescription() {
		return jobDescription;
	}

	public void setJobDescription(String jobDescription) {
		this.jobDescription = jobDescription;
	}

	public String getSkillsRequired() {
		return skillsRequired;
	}

	public void setSkillsRequired(String skillsRequired) {
		this.skillsRequired = skillsRequired;
	}

	public String getExperienceRequired() {
		return experienceRequired;
	}

	public void setExperienceRequired(String experienceRequired) {
		this.experienceRequired = experienceRequired;
	}

	public String getJoiningType() {
		return joiningType;
	}

	public void setJoiningType(String joiningType) {
		this.joiningType = joiningType;
	}

	public String getJobType() {
		return jobType;
	}

	public void setJobType(String jobType) {
		this.jobType = jobType;
	}

	public LocalDate getLastDateToApply() {
		return lastDateToApply;
	}

	public void setLastDateToApply(LocalDate lastDateToApply) {
		this.lastDateToApply = lastDateToApply;
	}

	public String getCompanyLink() {
		return companyLink;
	}

	public void setCompanyLink(String companyLink) {
		this.companyLink = companyLink;
	}

	public String getCompanyEmail() {
		return companyEmail;
	}

	public void setCompanyEmail(String companyEmail) {
		this.companyEmail = companyEmail;
	}

	public String getPosterDepartment() {
		return posterDepartment;
	}

	public void setPosterDepartment(String posterDepartment) {
		this.posterDepartment = posterDepartment;
	}

	public String getPosterBatchYear() {
		return posterBatchYear;
	}

	public void setPosterBatchYear(String posterBatchYear) {
		this.posterBatchYear = posterBatchYear;
	}

	public Long getId() {
		return id;
	}

	public String getPhotoUrl() { return photoUrl; } public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; } public void setId(Long id) {
		this.id = id;
	}

}
