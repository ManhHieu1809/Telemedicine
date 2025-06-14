package com.hospital.telemedicine.dto.request;

import lombok.Data;

@Data
public class UpdateReviewRequest {
    private int rating;
    private String comment;
}
