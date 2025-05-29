package com.hospital.telemedicine.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
@Data
public class FavoriteDoctorResponse {
    private Long doctorId;
    private Long doctorUserId;
    private String doctorName;
    private String avatarUrl;
    private String specialty;
    private double averageRating;
    private int totalReviews;
    private LocalDateTime createdAt;
    private boolean success;


    public FavoriteDoctorResponse(Long doctorId, Long doctorUserId, String doctorName, String specialty, LocalDateTime createdAt) {
        this.doctorId = doctorId;
        this.doctorUserId = doctorUserId;
        this.doctorName = doctorName;
        this.specialty = specialty;
        this.createdAt = createdAt;
        this.success = true;
    }

    // Constructor cho trường hợp thất bại
    public FavoriteDoctorResponse() {
        this.success = false;
    }
}
