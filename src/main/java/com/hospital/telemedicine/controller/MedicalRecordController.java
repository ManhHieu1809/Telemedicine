package com.hospital.telemedicine.controller;

import com.hospital.telemedicine.dto.request.MedicalRecordRequest;
import com.hospital.telemedicine.dto.response.ApiResponse;
import com.hospital.telemedicine.dto.response.MedicalRecordResponse;
import com.hospital.telemedicine.service.MedicalRecordService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.mail.MessagingException;
import java.util.List;

@RestController
@RequestMapping("/api/medical-records")
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;

    public MedicalRecordController(MedicalRecordService medicalRecordService) {
        this.medicalRecordService = medicalRecordService;
    }

    // Endpoint gốc (không có kiểm tra thuốc)
    @PostMapping
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<MedicalRecordResponse>> createMedicalRecord(@RequestBody MedicalRecordRequest request) throws MessagingException {
        MedicalRecordResponse response = medicalRecordService.createMedicalRecord(request);
        return ResponseEntity.ok(new ApiResponse<>(true, response));
    }

    // Endpoint mới với kiểm tra thuốc thông minh
    @PostMapping("/smart")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<MedicalRecordResponse>> createSmartMedicalRecord(@RequestBody MedicalRecordRequest request) throws MessagingException {
        MedicalRecordResponse response = medicalRecordService.createMedicalRecordWithDrugCheck(request);
        return ResponseEntity.ok(new ApiResponse<>(response.isSuccess(), response));
    }

    // Endpoint để force create (bỏ qua cảnh báo)
    @PostMapping("/force")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<MedicalRecordResponse>> forceCreateMedicalRecord(@RequestBody MedicalRecordRequest request) throws MessagingException {
        // Thêm flag để bỏ qua cảnh báo
        request.setIgnoreWarnings(true);
        MedicalRecordResponse response = medicalRecordService.createMedicalRecord(request);
        return ResponseEntity.ok(new ApiResponse<>(true, response));
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<List<MedicalRecordResponse>>> getMedicalRecordsByPatient(@PathVariable Long patientId) {
        List<MedicalRecordResponse> records = medicalRecordService.getMedicalRecordsByPatient(patientId);
        return ResponseEntity.ok(new ApiResponse<>(true, records));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<MedicalRecordResponse>> updateMedicalRecord(@PathVariable Long id, @RequestBody MedicalRecordRequest request) throws MessagingException {
        MedicalRecordResponse response = medicalRecordService.updateMedicalRecord(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, response));
    }

    // Endpoint cập nhật với kiểm tra thuốc
    @PutMapping("/{id}/smart")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<MedicalRecordResponse>> updateSmartMedicalRecord(@PathVariable Long id, @RequestBody MedicalRecordRequest request) throws MessagingException {
        MedicalRecordResponse response = medicalRecordService.updateMedicalRecordWithDrugCheck(id, request);
        return ResponseEntity.ok(new ApiResponse<>(response.isSuccess(), response));
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<String> getMedicalRecordPdf(@PathVariable Long id) {
        String latexContent = medicalRecordService.generateMedicalRecordPdf(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/x-tex"))
                .body(latexContent);
    }

    @PostMapping("/{id}/email")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<MedicalRecordResponse>> sendMedicalRecordByEmail(@PathVariable Long id) {
        MedicalRecordResponse response = medicalRecordService.sendMedicalRecordByEmail(id);
        return ResponseEntity.ok(new ApiResponse<>(true, response));
    }
}