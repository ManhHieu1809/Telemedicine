package com.hospital.telemedicine.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateUserRequest {
    private String fullName;
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;
    private String avatarUrl;
    private String phone;
    private String address;
    private LocalDate dateOfBirth; // Chỉ cho bệnh nhân
    private String gender; // Chỉ cho bệnh nhân
    private String specialty; // Chỉ cho bác sĩ
    private Integer experience; // Chỉ cho bác sĩ
}
