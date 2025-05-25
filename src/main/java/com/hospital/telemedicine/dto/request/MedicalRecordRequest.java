package com.hospital.telemedicine.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class MedicalRecordRequest {
    private Long patientId;
    private Long doctorId;
    private String diagnosis;
    private String notes;
    private List<PrescriptionDetailRequest> prescriptionDetails;
}
