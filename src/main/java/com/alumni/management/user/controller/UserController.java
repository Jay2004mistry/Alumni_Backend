package com.alumni.management.user.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alumni.management.user.dto.ForgotPasswordRequestDto;
import com.alumni.management.user.dto.LoginRequestDto;
import com.alumni.management.user.dto.LoginResponseDto;
import com.alumni.management.user.dto.ResetPasswordRequestDto;
import com.alumni.management.user.dto.UserResponseDto;
import com.alumni.management.user.entity.User;
import com.alumni.management.user.service.UserService;

@RestController
@RequestMapping("/api/users")
public class UserController {

	@Autowired
	UserService userService;

	@PostMapping
	public User createUser(@RequestBody User user) {
		return userService.createUser(user);
	}

	@GetMapping()
	public List<UserResponseDto> getAllUsers() {
		return userService.getAllUsers();
	}

	@GetMapping("/{id}")
	public UserResponseDto getUserById(@PathVariable Long id) {
		return userService.getUserById(id);
	}

	@DeleteMapping("/{id}")
	@org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
	public String deleteUser(@PathVariable Long id) {
		userService.deleteUser(id);
		return "Delete Successfully";
	}

	@PutMapping("/{id}")
	public User updateUserData(@PathVariable Long id, @RequestBody User user) {
		return userService.updateUserData(id, user);
	}

	@PostMapping("/login")
	public LoginResponseDto login(@RequestBody LoginRequestDto request) {
	    return userService.login(request);
	}

	@GetMapping("/me")
	public UserResponseDto getCurrentUser() {
		return userService.getCurrentUserProfile();
	}

	@PostMapping("/refresh-token")
	public java.util.Map<String, String> refreshToken() {
		return userService.refreshToken();
	}

	@GetMapping("/search/name/{name}")
	public List<UserResponseDto> searchUsersByName(@PathVariable String name) {
	    return userService.searchUsersByName(name);
	}

	@PostMapping("/profile-image")
	public java.util.Map<String, String> uploadProfileImage(@org.springframework.web.bind.annotation.RequestParam("image") org.springframework.web.multipart.MultipartFile image) {
		try {
			String url = com.alumni.management.security.FileUploadSecurityUtil.saveImageSafely(image, "uploads/profiles/");
			java.util.Map<String, String> res = new java.util.HashMap<>();
			res.put("url", url);
			return res;
		} catch (java.io.IOException e) {
			throw new RuntimeException("Failed to save profile image", e);
		}
	}

	@PostMapping("/forgot-password")
	public java.util.Map<String, String> forgotPassword(@RequestBody ForgotPasswordRequestDto request) {
		return userService.processForgotPassword(request);
	}

	@PostMapping("/reset-password")
	public java.util.Map<String, String> resetPassword(@RequestBody ResetPasswordRequestDto request) {
		return userService.resetPassword(request);
	}

}
