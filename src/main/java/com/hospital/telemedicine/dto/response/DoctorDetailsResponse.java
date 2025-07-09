package com.hospital.telemedicine.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DoctorDetailsResponse {
    private Long doctorId;
    private String fullName;
    private String specialty;
    private Integer experience;
    private String phone;
    private String address;
    private String avatarUrl;
    private double averageRating;
    private int totalReviews;
    private List<ReviewInfo> reviews;

    @Data
    public static class ReviewInfo {
        private Long reviewId;
        private Long patientId;
        private String patientName; // Tên bệnh nhân viết review
        private String avatarUrl; // Ảnh đại diện của bệnh nhân
        private int rating;
        private String comment;
        private LocalDateTime createdAt;
    }
}
