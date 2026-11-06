package com.alumni.management.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeviceRegistrationRequest {

    @NotBlank(message = "FCM token must not be blank")
    private String token;

    private String deviceType = "ANDROID";

    private String deviceId;
}

