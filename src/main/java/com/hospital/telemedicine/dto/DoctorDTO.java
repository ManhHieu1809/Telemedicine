package com.hospital.telemedicine.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoctorDTO {
    private Long id;
    private Long userId;
    private String username;
    private String fullName;
    private String avatarUrl;
    private String status; // ONLINE, OFFLINE
}
