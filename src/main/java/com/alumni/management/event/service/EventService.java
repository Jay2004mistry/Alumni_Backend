package com.alumni.management.event.service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.alumni.management.event.entity.Event;
import com.alumni.management.event.repository.EventRepository;
import com.alumni.management.exception.ResourceNotFoundException;
import com.alumni.management.user.entity.User;
import com.alumni.management.user.repository.UserRepository;

import com.alumni.management.notification.entity.Notification;
import com.alumni.management.notification.repository.NotificationRepository;

import com.alumni.management.event.dto.EventRegistrationResponse;
import com.alumni.management.event.entity.EventRegistration;
import com.alumni.management.event.repository.EventRegistrationRepository;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventService {

	@Autowired
	EventRepository eventRepository;

	@Autowired
	UserRepository userRepository;

	@Autowired
	EventRegistrationRepository eventRegistrationRepository;	

	@Autowired
	NotificationRepository notificationRepository;

//	jwt authentication for token genration

	private User getCurrentUser() {
		String email = SecurityContextHolder.getContext().getAuthentication().getName();
		return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
	}

//	//	Add this new 1 if code not work remove it untill 		return "Event submitted for approval";

	public String createEvent(String title, String description, String location, LocalDate eventDate,
			String targetDepartment, String note, MultipartFile image) {
		User user = getCurrentUser();

		Event event = new Event();
		event.setTitle(title);
		event.setDescription(description);
		event.setLocation(location);
		event.setEventDate(eventDate);
		event.setTargetDepartment(targetDepartment);
		event.setNote(note);
		event.setStatus("APPROVED");
		event.setCreatedAt(LocalDateTime.now());
		event.setCreatedBy(user);

// Handle image upload if present
		if (image != null && !image.isEmpty()) {
			try {
				String imageUrl = com.alumni.management.security.FileUploadSecurityUtil.saveImageSafely(image, "uploads/events/");
				event.setImageUrl(imageUrl);
			} catch (IOException e) {
				throw new RuntimeException("Failed to upload image", e);
			}
		}

		eventRepository.save(event);

		try {
			Notification notification = new Notification();
			notification.setTitle("New Event Posted");
			notification.setMessage(event.getTitle() + " has been announced by " + user.getName());
			notification.setTimestamp(LocalDateTime.now());
			notification.setRead(false);
			notificationRepository.save(notification);
		} catch (Exception e) {
			// Silently fail notification so it doesn't block main flow
		}

		return "Event submitted for approval";
	}

//	public String createEvent(Event event) {
//User user=getCurrentUser();
//		Event event2 = new Event();
//		event2.setTitle(event.getTitle());
//		event2.setDescription(event.getDescription());
//		event2.setLocation(event.getLocation());
//		event2.setEventDate(event.getEventDate());
//
//		event2.setStatus("PENDING");
//		event2.setCreatedAt(LocalDateTime.now());
//		event2.setCreatedBy(user);
//
//		eventRepository.save(event2);
//
//		return "Event submitted for approval";
//
//	}

	public List<Event> getApprovedEvents() {
		return eventRepository.findByStatus("APPROVED");
	}

	public List<Event> getMyEvents() {

		User user = getCurrentUser();
		userRepository.findById(user.getId()).orElseThrow(() -> new ResourceNotFoundException("User not found"));

		return eventRepository.findByCreatedBy_Id(user.getId());
	}

	public EventRegistrationResponse registerForEvent(Long eventId) {
		User user = getCurrentUser();

		Event event = eventRepository.findById(eventId).orElseThrow(() -> new ResourceNotFoundException("Event not found."));

		if (event.getCreatedBy() !=  null
			&& event.getCreatedBy().getId().equals(user.getId())) {
				throw new RuntimeException("Organizers cannot register for their own event.");
		}

		if (!"APPROVED".equalsIgnoreCase(event.getStatus())) {
			throw new RuntimeException("This event is not open for registration.");
		}

		if (event.getEventDate() != null && event.getEventDate().isBefore(LocalDate.now())) {
			throw new RuntimeException("Rgistration for this event is closed.");
		}

		if (eventRegistrationRepository.existsByEvent_IdAndUser_Id(eventId, user.getId())) {
			throw new RuntimeException("You are already registred for this event.");
		}

		EventRegistration registration = 
			new EventRegistration(event, user, LocalDateTime.now());

		EventRegistration saved = eventRegistrationRepository.save(registration);

		return new EventRegistrationResponse(
			saved.getId(),
			user.getId(),
			user.getRole() != null
				? user.getRole().getRoleName()
				:null,
			user.getName(),
			user.getEmail(),
			user.getUniversityNumber(),
			saved.getRegisteredAt()
		);
	}

	public boolean isUserRegisteredForEvent(Long eventId) {

		User user = getCurrentUser();

		if(!eventRepository.existsById(eventId)) {
			throw new ResourceNotFoundException("Event not found.");
		}

		return eventRegistrationRepository.existsByEvent_IdAndUser_Id(eventId, user.getId());

	}

	public List<EventRegistrationResponse> getEventRegistrations(Long eventId) {
		User currentUser = getCurrentUser();

		Event event = eventRepository.findById(eventId)
			.orElseThrow(() -> new ResourceNotFoundException("Event not found."));

		boolean isOwner = event.getCreatedBy() != null
			&& event.getCreatedBy().getId().equals(currentUser.getId());

		boolean isAdmin = currentUser.getRole() != null
			&& "ADMIN".equalsIgnoreCase(currentUser.getRole().getRoleName());

		if (!isOwner && !isAdmin) {
			throw new RuntimeException("You are not authorized to view registrations for this event");
		}

		return eventRegistrationRepository
			.findByEvent_IdOrderByRegisteredAtAsc(eventId)
			.stream()
			.map(registration -> new EventRegistrationResponse(
				registration.getId(),
				registration.getUser().getId(),
				registration.getUser().getRole() != null
					? registration.getUser().getRole().getRoleName()
					: null,
				registration.getUser().getName(),
				registration.getUser().getEmail(),
				registration.getUser().getUniversityNumber(),
				registration.getRegisteredAt()
			))
			.toList();


	}

	public List<Event> getPendingEvents() {
		return eventRepository.findByStatus("PENDING");
	}

	public String approveEvent(Long eventId) {

		Event event = eventRepository.findById(eventId)
				.orElseThrow(() -> new ResourceNotFoundException("Event not found"));

		event.setStatus("APPROVED");
		eventRepository.save(event);

		return "Event approved successfully";
	}

	public String rejectEvent(Long eventId) {

		Event event = eventRepository.findById(eventId)
				.orElseThrow(() -> new ResourceNotFoundException("Event not found"));

		event.setStatus("REJECTED");
		eventRepository.save(event);

		return "Event rejected successfully";
	}

	@Transactional
	public String deleteEvent(Long eventId) {
		Event event = eventRepository.findById(eventId)
				.orElseThrow(() -> new ResourceNotFoundException("Event not found with id " + eventId));
		
		eventRegistrationRepository.deleteByEvent_Id(eventId);
		eventRepository.delete(event);
		return "Event deleted successfully";
	}

	public String updateEvent(Long id, String title, String description, String location, LocalDate eventDate,
			String targetDepartment, String note, MultipartFile image) {
		User currentUser = getCurrentUser();
		Event existingEvent = eventRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Event not found with id " + id));

		boolean isOwner = existingEvent.getCreatedBy() != null && existingEvent.getCreatedBy().getId().equals(currentUser.getId());
		boolean isAdmin = currentUser.getRole() != null && "ADMIN".equalsIgnoreCase(currentUser.getRole().getRoleName());
		if (!isOwner && !isAdmin) {
			throw new RuntimeException("You are not authorized to update this event");
		}

		existingEvent.setTitle(title);
		existingEvent.setDescription(description);
		existingEvent.setLocation(location);
		existingEvent.setEventDate(eventDate);
		existingEvent.setTargetDepartment(targetDepartment);
		existingEvent.setNote(note);

		if (image != null && !image.isEmpty()) {
			try {
				String imageUrl = com.alumni.management.security.FileUploadSecurityUtil.saveImageSafely(image, "uploads/events/");
				existingEvent.setImageUrl(imageUrl);
			} catch (IOException e) {
				throw new RuntimeException("Failed to upload image", e);
			}
		}

		eventRepository.save(existingEvent);
		return "Event updated successfully";
	}

}

