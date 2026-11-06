package com.alumni.management.notification.entity;

import java.time.Instant;

import com.alumni.management.user.entity.User;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "device_tokens",
    indexes = {
        @Index(name = "idx_device_token_user_email", columnList = "userEmail"),
        @Index(name = "idx_device_token_val", columnList = "token", unique = true)
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeviceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    @Column(nullable = false)
    private String userEmail;

    @Column(nullable = false, unique = true, length = 512)
    private String token;

    @Column(length = 32)
    private String deviceType = "ANDROID";

    @Column(length = 128)
    private String deviceId;

    private boolean isActive = true;

    private Instant createdAt = Instant.now();

    private Instant updatedAt = Instant.now();
}

