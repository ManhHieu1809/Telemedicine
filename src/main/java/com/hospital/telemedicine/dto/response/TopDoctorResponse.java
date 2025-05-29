package com.hospital.telemedicine.dto.response;

import lombok.Data;

@Data
public class TopDoctorResponse {
    private Long id;
    private String fullName;
    private String avatarUrl;
    private String specialty;
    private double averageRating;
    private int totalReviews;
}
