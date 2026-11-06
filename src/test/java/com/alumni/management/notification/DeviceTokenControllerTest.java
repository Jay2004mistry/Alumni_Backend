package com.alumni.management.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.alumni.management.notification.controller.DeviceTokenController;
import com.alumni.management.notification.dto.DeviceRegistrationRequest;
import com.alumni.management.notification.dto.DeviceUnregistrationRequest;
import com.alumni.management.notification.entity.DeviceToken;
import com.alumni.management.notification.service.PushNotificationService;

@ExtendWith(MockitoExtension.class)
public class DeviceTokenControllerTest {

    @Mock
    private PushNotificationService pushNotificationService;

    @InjectMocks
    private DeviceTokenController deviceTokenController;

    @Test
    @DisplayName("TC-01: Authenticated device registration endpoint")
    void testRegisterDeviceEndpoint() {
        Principal principal = () -> "vivek@example.com";
        DeviceRegistrationRequest request = new DeviceRegistrationRequest("sample_fcm_token_123", "ANDROID", "pixel_8");

        DeviceToken created = new DeviceToken();
        created.setUserEmail("vivek@example.com");
        created.setToken("sample_fcm_token_123");
        created.setDeviceType("ANDROID");
        created.setDeviceId("pixel_8");
        created.setUpdatedAt(Instant.now());

        when(pushNotificationService.registerDeviceToken(eq("vivek@example.com"), any(DeviceRegistrationRequest.class)))
                .thenReturn(created);

        ResponseEntity<?> response = deviceTokenController.registerDevice(principal, request);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrueMap(response.getBody(), "status", "success");
    }

    @Test
    @DisplayName("TC-29: Unauthenticated device registration is rejected")
    void testUnauthenticatedRegistrationRejected() {
        DeviceRegistrationRequest request = new DeviceRegistrationRequest("token_xyz", "ANDROID", "pixel_8");

        ResponseEntity<?> response = deviceTokenController.registerDevice(null, request);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    @DisplayName("TC-05: Authenticated device unregistration endpoint")
    void testUnregisterDeviceEndpoint() {
        Principal principal = () -> "vivek@example.com";
        DeviceUnregistrationRequest request = new DeviceUnregistrationRequest("sample_fcm_token_123");

        ResponseEntity<?> response = deviceTokenController.unregisterDevice(principal, request);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(pushNotificationService).unregisterDeviceToken("vivek@example.com", "sample_fcm_token_123");
    }

    private void assertTrueMap(Object body, String key, String expectedValue) {
        if (body instanceof Map<?, ?> map) {
            assertEquals(expectedValue, map.get(key));
        } else {
            throw new AssertionError("Response body is not a Map");
        }
    }
}

