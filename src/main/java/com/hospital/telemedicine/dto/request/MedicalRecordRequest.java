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

    // Thêm field mới để điều khiển kiểm tra thuốc
    private boolean ignoreWarnings = false; // Bỏ qua cảnh báo khi tạo đơn thuốc
    private boolean checkDrugInteractions = true; // Kiểm tra tương tác thuốc
    private boolean suggestGeneric = true; // Gợi ý thuốc generic
}