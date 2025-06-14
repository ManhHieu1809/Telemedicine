package com.hospital.telemedicine.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class SystemReportResponse {
    private long totalAppointments;
    private double totalRevenue;
    private List<DoctorRating> doctorRatings;

    @Data
    public static class DoctorRating {
        private Long doctorId;
        private String fullName;
        private double averageRating;
        private int totalReviews;
    }
}