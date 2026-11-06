package com.alumni.management.notification.controller;

import java.security.Principal;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alumni.management.notification.dto.DeviceRegistrationRequest;
import com.alumni.management.notification.dto.DeviceUnregistrationRequest;
import com.alumni.management.notification.entity.DeviceToken;
import com.alumni.management.notification.service.PushNotificationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/notifications/devices")
public class DeviceTokenController {

    private static final Logger log = LoggerFactory.getLogger(DeviceTokenController.class);

    @Autowired
    private PushNotificationService pushNotificationService;

    private String getAuthenticatedUserEmail(Principal principal) {
        if (principal != null && principal.getName() != null && !principal.getName().trim().isEmpty()) {
            return principal.getName().trim().toLowerCase();
        }
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null && !auth.getName().equalsIgnoreCase("anonymousUser")) {
            return auth.getName().trim().toLowerCase();
        }
        return "";
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerDevice(Principal principal, @Valid @RequestBody DeviceRegistrationRequest request) {
        String authEmail = getAuthenticatedUserEmail(principal);
        if (authEmail.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }

        try {
            DeviceToken token = pushNotificationService.registerDeviceToken(authEmail, request);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Device token registered successfully",
                    "deviceType", token.getDeviceType(),
                    "registeredAt", token.getUpdatedAt().toString()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to register device token for user {}: {}", authEmail, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to register device token"));
        }
    }

    @PostMapping("/unregister")
    public ResponseEntity<?> unregisterDevice(Principal principal, @Valid @RequestBody DeviceUnregistrationRequest request) {
        String authEmail = getAuthenticatedUserEmail(principal);
        if (authEmail.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }

        try {
            pushNotificationService.unregisterDeviceToken(authEmail, request.getToken());
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Device token unregistered successfully"
            ));
        } catch (Exception e) {
            log.error("Failed to unregister device token for user {}: {}", authEmail, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to unregister device token"));
        }
    }
}

