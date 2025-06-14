package com.hospital.telemedicine.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MedicalRecordResponse {
    private Long id;
    private Long patientId;
    private String patientName;
    private Long doctorId;
    private String doctorName;
    private String diagnosis;
    private String notes;
    private LocalDateTime prescribedDate;
    private List<PrescriptionDetailResponse> prescriptionDetails;
    private LocalDateTime recordDate;
    private boolean success;
    private String message;

    // Thêm field mới cho phân tích thuốc
    private SmartPrescriptionResponse drugAnalysis;

    // Constructor cho lỗi
    public MedicalRecordResponse(String message) {
        this.success = false;
        this.message = message;
    }

    // Constructor đầy đủ (cập nhật)
    public MedicalRecordResponse(Long id, Long patientId, String patientName, Long doctorId, String doctorName,
                                 String diagnosis, String notes, LocalDateTime recordDate, LocalDateTime prescribedDate,
                                 List<PrescriptionDetailResponse> prescriptionDetails, boolean success, String message) {
        this.id = id;
        this.patientId = patientId;
        this.patientName = patientName;
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.diagnosis = diagnosis;
        this.notes = notes;
        this.recordDate = recordDate;
        this.prescribedDate = prescribedDate;
        this.prescriptionDetails = prescriptionDetails;
        this.success = success;
        this.message = message;
    }

    // Constructor với drug analysis
    public MedicalRecordResponse(Long id, Long patientId, String patientName, Long doctorId, String doctorName,
                                 String diagnosis, String notes, LocalDateTime recordDate, LocalDateTime prescribedDate,
                                 List<PrescriptionDetailResponse> prescriptionDetails, boolean success, String message,
                                 SmartPrescriptionResponse drugAnalysis) {
        this(id, patientId, patientName, doctorId, doctorName, diagnosis, notes, recordDate, prescribedDate, prescriptionDetails, success, message);
        this.drugAnalysis = drugAnalysis;
    }
}