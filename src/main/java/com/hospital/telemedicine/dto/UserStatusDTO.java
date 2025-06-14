package com.hospital.telemedicine.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserStatusDTO {
    private Long userId;
    private String status;
    private LocalDateTime lastActive;
    private String username;
}
