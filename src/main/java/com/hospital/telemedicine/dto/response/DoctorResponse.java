package com.hospital.telemedicine.dto.response;

import lombok.Data;

@Data
public class DoctorResponse {
    private Long id;
    private Long userId;
    private String fullName;
    private String email;
    private String avatarUrl;
    private String specialty;
    private Integer experience;
    private String phone;
    private String address;
    private boolean isFavorite;
}
