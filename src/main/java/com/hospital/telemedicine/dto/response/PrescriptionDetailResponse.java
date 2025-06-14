package com.hospital.telemedicine.dto.response;

import lombok.Data;

@Data
public class PrescriptionDetailResponse {
    private Long id;
    private String medicationName;
    private String dosage;
    private String frequency;
    private String duration;
    private String instructions;
}
