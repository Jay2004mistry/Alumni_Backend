package com.alumni.management.chat.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PresenceEvent {
    private String userEmail;
    private String status; // "ONLINE" or "OFFLINE"

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant lastSeen;
}
