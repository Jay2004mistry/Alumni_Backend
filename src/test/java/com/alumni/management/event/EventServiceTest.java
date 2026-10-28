package com.alumni.management.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.alumni.management.event.entity.Event;
import com.alumni.management.event.repository.EventRepository;
import com.alumni.management.event.service.EventService;
import com.alumni.management.role.entity.Role;
import com.alumni.management.user.entity.User;
import com.alumni.management.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class EventServiceTest {

	@Mock
	private EventRepository eventRepository;

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private EventService eventService;

	private User facultyUser;
	private Role facultyRole;
	private Event event;

	@BeforeEach
	void setUp() {
		facultyRole = new Role("FACULTY");
		facultyRole.setId(3L);

		facultyUser = new User(112L, "Prerna Agarwal", "vivekposhiya82@gmail.com", "password", facultyRole);

		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(facultyUser.getEmail(), "password")
		);

		event = new Event();
		event.setId(40L);
		event.setTitle("Machine Learning & AI in Production Seminar");
		event.setDescription("Exploring end-to-end Machine Learning pipelines");
		event.setLocation("Central Conference Hall, GLS University");
		event.setEventDate(LocalDate.of(2026, 12, 12));
		event.setStatus("APPROVED");
		event.setCreatedAt(LocalDateTime.now());
		event.setCreatedBy(facultyUser);
		event.setImageUrl("/uploads/events/1789801316903_1 (2).jpg");
		event.setTargetDepartment("MCA");
		event.setNote("Prerequisites: Basic knowledge of Python and mathematics.");
	}

	@Test
	void testGetApprovedEventsReturnsApprovedEventsWithOrganizer() {
		when(eventRepository.findByStatus("APPROVED")).thenReturn(List.of(event));

		List<Event> approvedEvents = eventService.getApprovedEvents();

		assertNotNull(approvedEvents);
		assertEquals(1, approvedEvents.size());
		assertEquals("Machine Learning & AI in Production Seminar", approvedEvents.get(0).getTitle());
		assertEquals("APPROVED", approvedEvents.get(0).getStatus());
		assertNotNull(approvedEvents.get(0).getCreatedBy());
		assertEquals("Prerna Agarwal", approvedEvents.get(0).getCreatedBy().getName());
		assertEquals("vivekposhiya82@gmail.com", approvedEvents.get(0).getCreatedBy().getEmail());
		assertEquals("/uploads/events/1789801316903_1 (2).jpg", approvedEvents.get(0).getImageUrl());
		verify(eventRepository).findByStatus("APPROVED");
	}

	@Test
	void testGetMyEventsReturnsEventsForCurrentOrganizer() {
		when(userRepository.findByEmail(facultyUser.getEmail())).thenReturn(Optional.of(facultyUser));
		when(userRepository.findById(112L)).thenReturn(Optional.of(facultyUser));
		when(eventRepository.findByCreatedBy_Id(112L)).thenReturn(List.of(event));

		List<Event> myEvents = eventService.getMyEvents();

		assertNotNull(myEvents);
		assertEquals(1, myEvents.size());
		assertEquals("Machine Learning & AI in Production Seminar", myEvents.get(0).getTitle());
		assertEquals(112L, myEvents.get(0).getCreatedBy().getId());
		verify(eventRepository).findByCreatedBy_Id(112L);
	}
}

