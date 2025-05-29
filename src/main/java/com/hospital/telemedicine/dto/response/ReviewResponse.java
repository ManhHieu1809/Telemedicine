package com.hospital.telemedicine.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
@Data
public class ReviewResponse {
    private Long id;
    private Long doctorId;
    private Long patientId;
    private String patientName;
    private int rating;
    private String comment;
    private LocalDateTime createdAt;
}
