package com.hospital.telemedicine.dto.request;

import lombok.Data;

@Data
public class UpdateDoctorRequest {
    private String username;
    private String email;
    private String fullName;
    private String specialty;
    private Integer experience;
    private String phone;
    private String address;
}