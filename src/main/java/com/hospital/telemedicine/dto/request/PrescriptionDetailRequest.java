package com.hospital.telemedicine.dto.request;

import lombok.Data;

@Data
public class PrescriptionDetailRequest {
    private String medicationName;
    private String dosage;
    private String frequency;
    private String duration;
    private String instructions;
}
