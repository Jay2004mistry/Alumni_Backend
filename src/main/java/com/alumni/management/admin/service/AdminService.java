package com.alumni.management.admin.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alumni.management.admin.dto.CreateUserRequestDto;
import com.alumni.management.alumni.entity.AlumniProfile;
import com.alumni.management.alumni.repository.AlumniProfileRepository;
import com.alumni.management.chat.entity.ChatMessage;
import com.alumni.management.chat.repository.ChatMessageRepository;
import com.alumni.management.event.entity.Event;
import com.alumni.management.event.repository.EventRepository;
import com.alumni.management.exception.ResourceNotFoundException;
import com.alumni.management.faculty.entity.FacultyProfile;
import com.alumni.management.faculty.repository.FacultyRepository;
import com.alumni.management.jobpost.entity.Job;
import com.alumni.management.jobpost.repository.JobRepository;
import com.alumni.management.post.entity.Post;
import com.alumni.management.post.repository.PostRepository;
import com.alumni.management.role.entity.Role;
import com.alumni.management.role.repository.RoleRepository;
import com.alumni.management.user.dto.UserResponseDto;
import com.alumni.management.user.entity.User;
import com.alumni.management.user.repository.UserRepository;

@Service
public class AdminService {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired(required = false)
	private AlumniProfileRepository alumniProfileRepository;

	@Autowired(required = false)
	private FacultyRepository facultyRepository;

	@Autowired(required = false)
	private JobRepository jobRepository;

	@Autowired(required = false)
	private EventRepository eventRepository;

	@Autowired(required = false)
	private PostRepository postRepository;

	@Autowired(required = false)
	private ChatMessageRepository chatMessageRepository;

	private String getUserDepartment(User user) {
		String role = user.getRole() != null ? user.getRole().getRoleName().toUpperCase() : "";
		if ("ALUMNI".equals(role) && alumniProfileRepository != null) {
			Optional<AlumniProfile> ap = alumniProfileRepository.findByUserId(user.getId());
			if (ap.isPresent() && ap.get().getDepartment() != null && !ap.get().getDepartment().isEmpty()) {
				return ap.get().getDepartment();
			}
		} else if ("FACULTY".equals(role) && facultyRepository != null) {
			Optional<FacultyProfile> fp = facultyRepository.findByUserId(user.getId());
			if (fp.isPresent() && fp.get().getDepartment() != null && !fp.get().getDepartment().isEmpty()) {
				return fp.get().getDepartment();
			}
		}
		return "MCA"; // Default department
	}

	public List<UserResponseDto> getAllUsers() {
		List<User> users = userRepository.findAll();
		return users.stream().map(user -> new UserResponseDto(
				user.getId(),
				user.getName(),
				user.getEmail(),
				user.getRole() != null ? user.getRole().getRoleName() : "STUDENT",
				getUserDepartment(user)
		)).collect(Collectors.toList());
	}

	public List<UserResponseDto> getUserByRole(String roleName) {
		String effectiveRole = roleName;
		if ("USER".equalsIgnoreCase(effectiveRole)) {
			effectiveRole = "STUDENT";
		}
		List<User> users = userRepository.findByRole_RoleName(effectiveRole);
		if (users.isEmpty()) {
			return List.of();
		}
		return users.stream().map(user -> new UserResponseDto(
				user.getId(),
				user.getName(),
				user.getEmail(),
				user.getRole() != null ? user.getRole().getRoleName() : "STUDENT",
				getUserDepartment(user)
		)).collect(Collectors.toList());
	}

	@Transactional
	public String deleteUser(Long userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with id " + userId));

		if (user.getRole() != null && "ADMIN".equalsIgnoreCase(user.getRole().getRoleName())) {
			throw new IllegalArgumentException("Admin accounts cannot be deleted through this endpoint.");
		}

		// 1. Delete associated profile
		if (alumniProfileRepository != null) {
			alumniProfileRepository.findByUserId(userId).ifPresent(alumniProfileRepository::delete);
		}
		if (facultyRepository != null) {
			facultyRepository.findByUserId(userId).ifPresent(facultyRepository::delete);
		}

		// 2. Delete associated jobs
		if (jobRepository != null) {
			List<Job> userJobs = jobRepository.findByUserId(userId);
			if (userJobs != null && !userJobs.isEmpty()) {
				jobRepository.deleteAll(userJobs);
			}
		}

		// 3. Delete associated events
		if (eventRepository != null) {
			List<Event> userEvents = eventRepository.findByCreatedBy_Id(userId);
			if (userEvents != null && !userEvents.isEmpty()) {
				eventRepository.deleteAll(userEvents);
			}
		}

		// 4. Delete associated posts
		if (postRepository != null) {
			List<Post> userPosts = postRepository.findByUserIdOrderByCreatedAtDesc(userId);
			if (userPosts != null && !userPosts.isEmpty()) {
				postRepository.deleteAll(userPosts);
			}
		}

		// 5. Clean up associated chat messages in MongoDB
		if (chatMessageRepository != null && user.getEmail() != null) {
			String email = user.getEmail().trim().toLowerCase();
			try {
				List<ChatMessage> msgs = chatMessageRepository.findBySenderIgnoreCaseOrReceiverIgnoreCase(email, email);
				if (msgs != null && !msgs.isEmpty()) {
					chatMessageRepository.deleteAll(msgs);
				}
			} catch (Exception ignored) {}
		}

		// 6. Delete user record from PostgreSQL
		userRepository.delete(user);
		return "User deleted successfully";
	}

	@Transactional
	public UserResponseDto createUserByAdmin(CreateUserRequestDto request) {
		String roleStr = request.getRoleName() != null ? request.getRoleName().toUpperCase().trim() : "STUDENT";
		if ("USER".equals(roleStr)) {
			roleStr = "STUDENT";
		}

		// Strictly forbid creating ADMIN accounts through this endpoint
		if ("ADMIN".equalsIgnoreCase(roleStr)) {
			throw new IllegalArgumentException("Creation of ADMIN accounts through this endpoint is not permitted.");
		}

		final String effectiveRole = roleStr;
		Role role = roleRepository.findByRoleName(effectiveRole)
				.orElseThrow(() -> new ResourceNotFoundException("Role " + effectiveRole + " not found"));

		User user = new User();
		user.setName(request.getName());
		user.setEmail(request.getEmail());
		user.setPassword(passwordEncoder.encode(request.getPassword()));
		user.setRole(role);

		User savedUser = userRepository.save(user);

		String dept = request.getDepartment() != null && !request.getDepartment().isEmpty() ? request.getDepartment() : "MCA";

		// Initialize profile if ALUMNI or FACULTY
		if ("ALUMNI".equals(effectiveRole) && alumniProfileRepository != null) {
			AlumniProfile ap = new AlumniProfile();
			ap.setUser(savedUser);
			ap.setDepartment(dept);
			alumniProfileRepository.save(ap);
		} else if ("FACULTY".equals(effectiveRole) && facultyRepository != null) {
			FacultyProfile fp = new FacultyProfile();
			fp.setUser(savedUser);
			fp.setEmail(savedUser.getEmail());
			fp.setDepartment(dept);
			facultyRepository.save(fp);
		}

		return new UserResponseDto(savedUser.getId(), savedUser.getName(), savedUser.getEmail(), savedUser.getRole().getRoleName(), dept);
	}
}
