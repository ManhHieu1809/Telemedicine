package com.hospital.telemedicine.dto.request;

import lombok.Data;

@Data
public class ReviewRequest {
    private Long appointmentId;
    private int rating;
    private String comment;
}
