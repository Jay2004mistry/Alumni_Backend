package com.alumni.management.user.dto;

public class UserResponseDto {

	private Long id;
	private String name;
	private String email;
	private String roleName;
	private String department;
	private String profilePictureUrl;

	public UserResponseDto() {
	}

	public UserResponseDto(Long id, String name, String email, String roleName) {
		this(id, name, email, roleName, "MCA", null);
	}

	public UserResponseDto(Long id, String name, String email, String roleName, String department) {
		this(id, name, email, roleName, department, null);
	}

	public UserResponseDto(Long id, String name, String email, String roleName, String department, String profilePictureUrl) {
		this.id = id;
		this.name = name;
		this.email = email;
		this.roleName = roleName;
		this.department = department != null && !department.isEmpty() ? department : "MCA";
		this.profilePictureUrl = profilePictureUrl;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getRoleName() {
		return roleName;
	}

	public void setRoleName(String roleName) {
		this.roleName = roleName;
	}

	public String getDepartment() {
		return department;
	}

	public void setDepartment(String department) {
		this.department = department;
	}

	public String getProfilePictureUrl() {
		return profilePictureUrl;
	}

	public void setProfilePictureUrl(String profilePictureUrl) {
		this.profilePictureUrl = profilePictureUrl;
	}
}
