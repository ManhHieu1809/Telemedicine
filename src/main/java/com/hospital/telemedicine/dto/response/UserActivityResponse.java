package com.hospital.telemedicine.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserActivityResponse {
    private Long userId;
    private String username;
    private String activityType; // LOGIN, APPOINTMENT, etc.
    private String description;
    private LocalDateTime timestamp;
}