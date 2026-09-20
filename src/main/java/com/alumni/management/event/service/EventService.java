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

@Service
public class EventService {

	@Autowired
	EventRepository eventRepository;

	@Autowired
	UserRepository userRepository;

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
// Create upload directory if it doesn't exist
				String uploadDir = "uploads/events/";
				File directory = new File(uploadDir);
				if (!directory.exists()) {
					directory.mkdirs();
				}

// Generate unique filename
				String fileName = System.currentTimeMillis() + "_" + image.getOriginalFilename();
				File destFile = new File(directory.getAbsolutePath() + File.separator + fileName);

// Save file
				image.transferTo(destFile);

// Set image URL (you can serve this statically)
				event.setImageUrl("/uploads/events/" + fileName);

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

	public String deleteEvent(Long eventId) {
		Event event = eventRepository.findById(eventId)
				.orElseThrow(() -> new ResourceNotFoundException("Event not found with id " + eventId));
		eventRepository.delete(event);
		return "Event deleted successfully";
	}

	public String updateEvent(Long id, String title, String description, String location, LocalDate eventDate,
			String targetDepartment, String note, MultipartFile image) {
		User currentUser = getCurrentUser();
		Event existingEvent = eventRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Event not found with id " + id));

		String userRole = currentUser.getRole().getRoleName();
		if (existingEvent.getCreatedBy() == null ||
				(!existingEvent.getCreatedBy().getId().equals(currentUser.getId()) && !"ADMIN".equals(userRole))) {
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
				String uploadDir = "uploads/events/";
				File directory = new File(uploadDir);
				if (!directory.exists()) {
					directory.mkdirs();
				}
				String fileName = System.currentTimeMillis() + "_" + image.getOriginalFilename();
				File destFile = new File(directory.getAbsolutePath() + File.separator + fileName);
				image.transferTo(destFile);
				existingEvent.setImageUrl("/uploads/events/" + fileName);
			} catch (IOException e) {
				throw new RuntimeException("Failed to upload image", e);
			}
		}

		eventRepository.save(existingEvent);
		return "Event updated successfully";
	}

}

