package com.alumni.management.event.dto;

import java.time.LocalDateTime; 
import com.alumni.management.event.entity.EventRegistration;

public class EventRegistrationResponse {

    private Long registrationId;
    private Long userId;
    private String userRole;
    private String name;
    private String email;
    private String universityNumber;
    private LocalDateTime registeredAt;

    public EventRegistrationResponse(
        Long registrationId,
        Long userId,
        String userRole,
        String name,
        String email,
        String universityNumber,
        LocalDateTime registeredAt 
    ) {
        this.registrationId = registrationId;
        this.userId = userId;
        this.userRole = userRole;
        this.name = name;
        this.email = email;
        this.universityNumber = universityNumber;
        this.registeredAt = registeredAt;
    }

    public Long getRegistrationId() {
        return registrationId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUserRole() {
        return userRole;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getUniversityNumber() {
        return universityNumber;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

}