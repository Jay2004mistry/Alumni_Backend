package com.alumni.management.event.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.alumni.management.event.entity.Event;
import com.alumni.management.event.service.EventService;

@RestController
@RequestMapping("api/events")
@CrossOrigin
public class EventController {

	@Autowired
	private EventService eventService;

//	Add this new 1 if code not work remove it
	@PostMapping(consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasAnyRole('ALUMNI', 'FACULTY', 'ADMIN')")
	public String createEvent(@RequestParam("title") String title, @RequestParam("description") String description,
			@RequestParam("location") String location,
			@RequestParam("eventDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate eventDate,
			@RequestParam("targetDepartment") String targetDepartment,
			@RequestParam(value = "note", required = false) String note,
			@RequestParam(value = "image", required = false) MultipartFile image) {

		return eventService.createEvent(title, description, location, eventDate, targetDepartment, note, image);
	}

	@org.springframework.web.bind.annotation.RequestMapping(value = "/{id}", method = {
			org.springframework.web.bind.annotation.RequestMethod.PUT,
			org.springframework.web.bind.annotation.RequestMethod.POST }, consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasAnyRole('ALUMNI', 'FACULTY', 'ADMIN')")
	public String updateEvent(@PathVariable Long id,
			@RequestParam("title") String title,
			@RequestParam("description") String description,
			@RequestParam("location") String location,
			@RequestParam("eventDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate eventDate,
			@RequestParam("targetDepartment") String targetDepartment,
			@RequestParam(value = "note", required = false) String note,
			@RequestParam(value = "image", required = false) MultipartFile image) {
		return eventService.updateEvent(id, title, description, location, eventDate, targetDepartment, note, image);
	}

	//	@PostMapping()
	//	public String createEvent(@RequestBody Event event) {
	//		return eventService.createEvent(event);
	//	}

//get approved event(publiclly)
	@GetMapping
	public List<Event> getApprovedEvents() {
		return eventService.getApprovedEvents();
	}

//My event
	@GetMapping("/my")
	public List<Event> getMyEvents() {
		return eventService.getMyEvents();
	}

//	Now only admin can access below 3
	@GetMapping("/admin/pending")
	public List<Event> getPendingEvents() {
		return eventService.getPendingEvents();
	}

//	@PreAuthorize("hasRole('ADMIN')") means only admin can do it
	@PreAuthorize("hasRole('ADMIN')")
	@PutMapping("/admin/approve/{eventId}")
	public String approveEvent(@PathVariable Long eventId) {
		return eventService.approveEvent(eventId);
	}

	@PreAuthorize("hasRole('ADMIN')")
	@PutMapping("/admin/reject/{eventId}")
	public String rejectEvent(@PathVariable Long eventId) {
		return eventService.rejectEvent(eventId);
	}

	@PreAuthorize("hasRole('ADMIN')")
	@org.springframework.web.bind.annotation.DeleteMapping("/admin/delete/{eventId}")
	public String deleteEvent(@PathVariable Long eventId) {
		return eventService.deleteEvent(eventId);
	}

}

