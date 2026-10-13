package com.alumni.management.verification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationResponseDto {

    private boolean success;
    private String message;
    private String email;
    private Integer cooldownSeconds;
}
