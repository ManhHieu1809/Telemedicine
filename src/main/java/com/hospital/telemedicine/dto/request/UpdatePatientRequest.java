package com.hospital.telemedicine.dto.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdatePatientRequest {
    private String username;
    private String email;
    private String fullName;
    private LocalDate dateOfBirth;
    private String gender;
    private String phone;
    private String address;
}