package com.hospital.telemedicine.controller;

import com.hospital.telemedicine.dto.request.MedicalRecordRequest;
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

    @PostMapping
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<MedicalRecordResponse> createMedicalRecord(@RequestBody MedicalRecordRequest request) throws MessagingException {
        MedicalRecordResponse response = medicalRecordService.createMedicalRecord(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<List<MedicalRecordResponse>> getMedicalRecordsByPatient(@PathVariable Long patientId) {
        List<MedicalRecordResponse> records = medicalRecordService.getMedicalRecordsByPatient(patientId);
        return ResponseEntity.ok(records);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<MedicalRecordResponse> updateMedicalRecord(@PathVariable Long id, @RequestBody MedicalRecordRequest request) throws MessagingException {
        MedicalRecordResponse response = medicalRecordService.updateMedicalRecord(id, request);
        return ResponseEntity.ok(response);
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
    public ResponseEntity<MedicalRecordResponse> sendMedicalRecordByEmail(@PathVariable Long id) {
        MedicalRecordResponse response = medicalRecordService.sendMedicalRecordByEmail(id);
        return ResponseEntity.ok(response);
    }
}
