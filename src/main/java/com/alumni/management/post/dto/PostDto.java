package com.alumni.management.post.dto;

import java.time.LocalDateTime;

public class PostDto {
	private Long id;
	private String content;
	private String imageUrl;
	private LocalDateTime createdAt;
	private Long userId;
	private String userName;
	private String userEmail;
	private String userRole;
	private String posterDepartment;
	private String posterDesignation;
	private String userProfilePictureUrl;

	public PostDto(Long id, String content, String imageUrl, LocalDateTime createdAt, Long userId, String userName,
			String userEmail, String userRole, String posterDepartment, String posterDesignation) {
		this(id, content, imageUrl, createdAt, userId, userName, userEmail, userRole, posterDepartment, posterDesignation, null);
	}

	public PostDto(Long id, String content, String imageUrl, LocalDateTime createdAt, Long userId, String userName,
			String userEmail, String userRole, String posterDepartment, String posterDesignation, String userProfilePictureUrl) {
		super();
		this.id = id;
		this.content = content;
		this.imageUrl = imageUrl;
		this.createdAt = createdAt;
		this.userId = userId;
		this.userName = userName;
		this.userEmail = userEmail;
		this.userRole = userRole;
		this.posterDepartment = posterDepartment;
		this.posterDesignation = posterDesignation;
		this.userProfilePictureUrl = userProfilePictureUrl;
	}

	public PostDto() {
		super();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
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

	public String getUserEmail() {
		return userEmail;
	}

	public void setUserEmail(String userEmail) {
		this.userEmail = userEmail;
	}

	public String getUserRole() {
		return userRole;
	}

	public void setUserRole(String userRole) {
		this.userRole = userRole;
	}

	public String getPosterDepartment() {
		return posterDepartment;
	}

	public void setPosterDepartment(String posterDepartment) {
		this.posterDepartment = posterDepartment;
	}

	public String getPosterDesignation() {
		return posterDesignation;
	}

	public void setPosterDesignation(String posterDesignation) {
		this.posterDesignation = posterDesignation;
	}

	public String getUserProfilePictureUrl() {
		return userProfilePictureUrl;
	}

	public void setUserProfilePictureUrl(String userProfilePictureUrl) {
		this.userProfilePictureUrl = userProfilePictureUrl;
	}
}
