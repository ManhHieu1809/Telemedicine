package com.hospital.telemedicine.dto.response;

import lombok.Data;

import java.time.LocalDate;
@Data
public class UserResponse {
    private Long userId;
    private String username;
    private String email;
    private String avatarUrl;
    private String role;
    private String fullName;
    private String phone;
    private String address;
    private LocalDate dateOfBirth; // Chỉ cho bệnh nhân
    private String gender; // Chỉ cho bệnh nhân
    private String specialty; // Chỉ cho bác sĩ
    private Integer experience; // Chỉ cho bác sĩ

}
