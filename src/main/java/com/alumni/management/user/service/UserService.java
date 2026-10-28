package com.alumni.management.user.service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.alumni.management.exception.ResourceNotFoundException;
import com.alumni.management.role.entity.Role;
import com.alumni.management.role.repository.RoleRepository;
import com.alumni.management.security.FileUploadSecurityUtil;
import com.alumni.management.security.JwtUtil;
import com.alumni.management.user.dto.LoginRequestDto;
import com.alumni.management.user.dto.LoginResponseDto;
import com.alumni.management.user.dto.UserResponseDto;
import com.alumni.management.user.entity.User;
import com.alumni.management.user.repository.UserRepository;
import com.alumni.management.verification.dto.VerificationResponseDto;
import com.alumni.management.verification.enums.VerificationPurpose;
import com.alumni.management.verification.exception.AccountNotVerifiedException;
import com.alumni.management.verification.exception.EmailDeliveryException;
import com.alumni.management.verification.exception.ResendCooldownException;
import com.alumni.management.verification.exception.UserAlreadyExistsException;

@Service
public class UserService {

	@Autowired
	UserRepository userRepository;
	@Autowired
	RoleRepository roleRepository;
	@Autowired
	PasswordEncoder passwordEncoder;
	@Autowired
	JwtUtil jwtUtil;
	@Autowired
	com.alumni.management.email.service.EmailService emailService;
	@Autowired
	com.alumni.management.verification.service.VerificationService verificationService;

	@Autowired(required = false)
	private com.alumni.management.alumni.repository.AlumniProfileRepository alumniProfileRepository;

	@Autowired(required = false)
	private com.alumni.management.student.repository.StudentProfileRepository studentProfileRepository;

	@Autowired(required = false)
	private com.alumni.management.faculty.repository.FacultyRepository facultyRepository;

	public String getUserProfilePicture(User user) {
		if (user == null || user.getId() == null || user.getRole() == null) {
			return null;
		}
		String role = user.getRole().getRoleName() != null ? user.getRole().getRoleName().toUpperCase() : "";
		if ("ALUMNI".equals(role) && alumniProfileRepository != null) {
			return alumniProfileRepository.findByUserId(user.getId())
					.map(com.alumni.management.alumni.entity.AlumniProfile::getProfilePictureUrl)
					.orElse(null);
		} else if ("STUDENT".equals(role) && studentProfileRepository != null) {
			return studentProfileRepository.findByUserId(user.getId())
					.map(com.alumni.management.student.entity.StudentProfile::getProfilePictureUrl)
					.orElse(null);
		} else if ("FACULTY".equals(role) && facultyRepository != null) {
			return facultyRepository.findByUserId(user.getId())
					.map(com.alumni.management.faculty.entity.FacultyProfile::getProfilePictureUrl)
					.orElse(null);
		}
		return null;
	}

	public String getUserDepartment(User user) {
		if (user == null || user.getId() == null || user.getRole() == null) {
			return "MCA";
		}
		String role = user.getRole().getRoleName() != null ? user.getRole().getRoleName().toUpperCase() : "";
		if ("ALUMNI".equals(role) && alumniProfileRepository != null) {
			return alumniProfileRepository.findByUserId(user.getId())
					.map(com.alumni.management.alumni.entity.AlumniProfile::getDepartment)
					.filter(d -> d != null && !d.isEmpty())
					.orElse("MCA");
		} else if ("STUDENT".equals(role) && studentProfileRepository != null) {
			return studentProfileRepository.findByUserId(user.getId())
					.map(com.alumni.management.student.entity.StudentProfile::getDepartment)
					.filter(d -> d != null && !d.isEmpty())
					.orElse("MCA");
		} else if ("FACULTY".equals(role) && facultyRepository != null) {
			return facultyRepository.findByUserId(user.getId())
					.map(com.alumni.management.faculty.entity.FacultyProfile::getDepartment)
					.filter(d -> d != null && !d.isEmpty())
					.orElse("MCA");
		}
		return "MCA";
	}

	private void ensureProfileExists(User user, String roleName) {
		String upperRole = roleName != null ? roleName.toUpperCase() : "ALUMNI";
		if ("ALUMNI".equals(upperRole) && alumniProfileRepository != null) {
			if (alumniProfileRepository.findByUserId(user.getId()).isEmpty()) {
				com.alumni.management.alumni.entity.AlumniProfile ap = new com.alumni.management.alumni.entity.AlumniProfile();
				ap.setUser(user);
				ap.setDepartment("MCA");
				alumniProfileRepository.save(ap);
			}
		} else if ("STUDENT".equals(upperRole) && studentProfileRepository != null) {
			if (studentProfileRepository.findByUserId(user.getId()).isEmpty()) {
				com.alumni.management.student.entity.StudentProfile sp = new com.alumni.management.student.entity.StudentProfile();
				sp.setUser(user);
				sp.setDepartment("MCA");
				studentProfileRepository.save(sp);
			}
		} else if ("FACULTY".equals(upperRole) && facultyRepository != null) {
			if (facultyRepository.findByUserId(user.getId()).isEmpty()) {
				com.alumni.management.faculty.entity.FacultyProfile fp = new com.alumni.management.faculty.entity.FacultyProfile();
				fp.setUser(user);
				fp.setDepartment("MCA");
				facultyRepository.save(fp);
			}
		}
	}

	public User createUser(User user) {
		String normalizedEmail = user.getEmail() != null ? user.getEmail().trim().toLowerCase(java.util.Locale.ROOT) : "";
		user.setEmail(normalizedEmail);

		java.util.Optional<User> existingUserOpt = userRepository.findByEmail(normalizedEmail);
		if (existingUserOpt.isPresent()) {
			User existingUser = existingUserOpt.get();
			if (existingUser.isVerified()) {
				throw new UserAlreadyExistsException("An account with this email already exists and is verified. Please log in.");
			}
			user.setId(existingUser.getId());
		}

		String roleName = user.getRole() != null && user.getRole().getRoleName() != null
				? user.getRole().getRoleName().toUpperCase()
				: "ALUMNI";
		Role role = roleRepository.findByRoleName(roleName)
				.orElseThrow(() -> new ResourceNotFoundException("Role not found"));

		user.setRole(role);
		user.setPassword(passwordEncoder.encode(user.getPassword()));
		user.setVerified(false);

		User saved = userRepository.save(user);

		ensureProfileExists(saved, roleName);

		verificationService.sendVerificationOtp(normalizedEmail, VerificationPurpose.ACCOUNT_REGISTRATION);
		return saved;
	}

	public List<UserResponseDto> getAllUsers() {
		List<User> users = userRepository.findAll();
		return users.stream().map(user -> new UserResponseDto(
				user.getId(),
				user.getName(),
				user.getEmail(),
				user.getRole() != null ? user.getRole().getRoleName() : "USER",
				getUserDepartment(user),
				getUserProfilePicture(user)))
				.collect(Collectors.toList());
	}

	public UserResponseDto getUserById(Long id) {
		User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
		return new UserResponseDto(
				user.getId(),
				user.getName(),
				user.getEmail(),
				user.getRole() != null ? user.getRole().getRoleName() : "USER",
				getUserDepartment(user),
				getUserProfilePicture(user));
	}

	public void deleteUser(Long id) {
		User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
		if (user.getRole() != null && "ADMIN".equalsIgnoreCase(user.getRole().getRoleName())) {
			throw new IllegalArgumentException("Admin accounts cannot be deleted");
		}
		userRepository.delete(user);
	}

	public User updateUserData(Long id, User user) {
		String currentEmail = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() != null
				? org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName()
				: null;
		if (currentEmail == null || "anonymousUser".equalsIgnoreCase(currentEmail)) {
			throw new SecurityException("Authentication required to update user data");
		}
		User currentUser = userRepository.findByEmail(currentEmail)
				.orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
		boolean isAdmin = currentUser.getRole() != null && "ADMIN".equalsIgnoreCase(currentUser.getRole().getRoleName());
		boolean isSelf = currentUser.getId().equals(id);

		if (!isAdmin && !isSelf) {
			throw new SecurityException("Unauthorized to update this user profile");
		}

		User existingUser = userRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		if (user.getName() != null && !user.getName().trim().isEmpty()) {
			existingUser.setName(user.getName().trim());
		}
		if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
			existingUser.setEmail(user.getEmail().trim().toLowerCase(java.util.Locale.ROOT));
		}
		if (user.getPassword() != null && !user.getPassword().trim().isEmpty()) {
			existingUser.setPassword(passwordEncoder.encode(user.getPassword().trim()));
		}

		// check role exist if yes then ready to update - ONLY ADMIN can change roles
		if (user.getRole() != null && user.getRole().getRoleName() != null) {
			if (isAdmin) {
				Role role = roleRepository.findByRoleName(user.getRole().getRoleName())
						.orElseThrow(() -> new ResourceNotFoundException("Role not found"));
				existingUser.setRole(role);
			}
		}

		return userRepository.save(existingUser);
	}

	public LoginResponseDto login(LoginRequestDto request) {
		String normalizedEmail = request.getEmail() != null ? request.getEmail().trim().toLowerCase(java.util.Locale.ROOT) : "";

		User user = userRepository.findByEmail(normalizedEmail)
				.orElseThrow(() -> new ResourceNotFoundException("Invalid email or password"));

		if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
			throw new ResourceNotFoundException("Invalid email or password");
		}

		if (!user.isVerified()) {
			boolean otpDispatched = false;
			int cooldownSeconds = 60;
			String message = "Account is not verified. A verification code has been sent to your email.";
			VerificationPurpose purpose = VerificationPurpose.ACCOUNT_ACTIVATION;

			if (verificationService != null) {
				try {
					VerificationResponseDto res = verificationService.sendVerificationOtp(user.getEmail(), purpose);
					otpDispatched = res.isSuccess();
					cooldownSeconds = res.getCooldownSeconds();
					if (res.getMessage() != null && !res.getMessage().isEmpty()) {
						message = "Account is not verified. " + res.getMessage();
					}
				} catch (ResendCooldownException rce) {
					otpDispatched = false;
					cooldownSeconds = (int) rce.getSecondsRemaining();
					message = "Account is not verified. " + rce.getMessage();
				} catch (EmailDeliveryException ede) {
					otpDispatched = false;
					cooldownSeconds = 0;
					message = "Account is not verified. " + ede.getMessage();
				} catch (Exception e) {
					otpDispatched = false;
					cooldownSeconds = 0;
					message = "Account is not verified. Unable to deliver verification email: " + e.getMessage();
				}
			}

			throw new AccountNotVerifiedException(message, user.getEmail(), otpDispatched, cooldownSeconds, purpose);
		}

		String token = jwtUtil.generateToken(user.getEmail());

		return new LoginResponseDto(
				token,
				user.getName(),
				user.getRole().getRoleName()
		);
	}

	public UserResponseDto getCurrentUserProfile() {
		String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() != null
				? org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName()
				: null;
		if (email == null || "anonymousUser".equalsIgnoreCase(email)) {
			throw new ResourceNotFoundException("No authenticated user found");
		}
		User user = userRepository.findByEmail(email.trim().toLowerCase(java.util.Locale.ROOT))
				.orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
		return new UserResponseDto(
				user.getId(),
				user.getName(),
				user.getEmail(),
				user.getRole() != null ? user.getRole().getRoleName() : "USER",
				getUserDepartment(user),
				getUserProfilePicture(user));
	}

	public String uploadProfileImage(MultipartFile image) throws IOException {
		String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() != null
				? org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName()
				: null;
		if (email == null || "anonymousUser".equalsIgnoreCase(email)) {
			throw new SecurityException("Authentication required to upload profile image");
		}
		User user = userRepository.findByEmail(email.trim().toLowerCase(java.util.Locale.ROOT))
				.orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

		String previousUrl = getUserProfilePicture(user);

		String newUrl = FileUploadSecurityUtil.saveImageSafely(image, "uploads/profiles/");

		String role = user.getRole() != null && user.getRole().getRoleName() != null
				? user.getRole().getRoleName().toUpperCase()
				: "";
		if ("ALUMNI".equals(role) && alumniProfileRepository != null) {
			var ap = alumniProfileRepository.findByUserId(user.getId()).orElseGet(() -> {
				var p = new com.alumni.management.alumni.entity.AlumniProfile();
				p.setUser(user);
				p.setDepartment("MCA");
				return p;
			});
			ap.setProfilePictureUrl(newUrl);
			alumniProfileRepository.save(ap);
		} else if ("STUDENT".equals(role) && studentProfileRepository != null) {
			var sp = studentProfileRepository.findByUserId(user.getId()).orElseGet(() -> {
				var p = new com.alumni.management.student.entity.StudentProfile();
				p.setUser(user);
				p.setDepartment("MCA");
				return p;
			});
			sp.setProfilePictureUrl(newUrl);
			studentProfileRepository.save(sp);
		} else if ("FACULTY".equals(role) && facultyRepository != null) {
			var fp = facultyRepository.findByUserId(user.getId()).orElseGet(() -> {
				var p = new com.alumni.management.faculty.entity.FacultyProfile();
				p.setUser(user);
				p.setDepartment("MCA");
				p.setDesignation("Faculty Member");
				p.setEmail(user.getEmail());
				return p;
			});
			fp.setProfilePictureUrl(newUrl);
			facultyRepository.save(fp);
		}

		if (previousUrl != null && !previousUrl.equals(newUrl)) {
			FileUploadSecurityUtil.deleteImageSafely(previousUrl);
		}

		return newUrl;
	}

	public java.util.Map<String, String> refreshToken() {
		String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() != null
				? org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName()
				: null;
		if (email == null || "anonymousUser".equalsIgnoreCase(email)) {
			throw new SecurityException("Authentication required to refresh session");
		}
		User user = userRepository.findByEmail(email.trim().toLowerCase(java.util.Locale.ROOT))
				.orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
		String newToken = jwtUtil.generateToken(user.getEmail());
		java.util.Map<String, String> response = new java.util.HashMap<>();
		response.put("token", newToken);
		response.put("name", user.getName());
		response.put("role", user.getRole() != null ? user.getRole().getRoleName() : "USER");
		response.put("email", user.getEmail());
		return response;
	}

	public List<UserResponseDto> searchUsersByName(String name) {
		List<User> users = userRepository.findByNameContainingIgnoreCase(name);
		return users.stream()
				.map(user -> new UserResponseDto(
						user.getId(),
						user.getName(),
						user.getEmail(),
						user.getRole() != null ? user.getRole().getRoleName() : "USER",
						getUserDepartment(user),
						getUserProfilePicture(user)))
				.collect(Collectors.toList());
	}

	public java.util.Map<String, String> processForgotPassword(com.alumni.management.user.dto.ForgotPasswordRequestDto request) {
		String email = request.getEmail() != null ? request.getEmail().toLowerCase().trim() : "";
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("User with email " + request.getEmail() + " not found"));

		verificationService.sendVerificationOtp(user.getEmail(), VerificationPurpose.PASSWORD_RESET);

		java.util.Map<String, String> response = new java.util.HashMap<>();
		response.put("message", "OTP sent successfully to your email");
		response.put("email", user.getEmail());
		return response;
	}

	public java.util.Map<String, String> resetPassword(com.alumni.management.user.dto.ResetPasswordRequestDto request) {
		String email = request.getEmail() != null ? request.getEmail().toLowerCase().trim() : "";
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("User with email " + email + " not found"));

		verificationService.verifyOtp(email, request.getOtp() != null ? request.getOtp().trim() : "", VerificationPurpose.PASSWORD_RESET);

		user.setPassword(passwordEncoder.encode(request.getNewPassword()));
		userRepository.save(user);

		java.util.Map<String, String> response = new java.util.HashMap<>();
		response.put("message", "Password reset successfully");
		return response;
	}
}
