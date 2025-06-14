package com.hospital.telemedicine.controller;

import com.hospital.telemedicine.dto.request.MedicalRecordRequest;
import com.hospital.telemedicine.dto.response.*;
import com.hospital.telemedicine.service.SmartPrescriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/drugs")
public class DrugController {

    @Autowired
    private SmartPrescriptionService smartPrescriptionService;

    /**
     * Tìm kiếm thuốc
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('DOCTOR', 'PATIENT')")
    public ResponseEntity<ApiResponse<List<DrugSuggestionResponse>>> searchDrugs(
            @RequestParam String query) {
        List<DrugSuggestionResponse> results = smartPrescriptionService.searchDrugs(query);
        return ResponseEntity.ok(new ApiResponse<>(true, results));
    }

    /**
     * Lấy thông tin chi tiết thuốc
     */
    @GetMapping("/details")
    @PreAuthorize("hasAnyRole('DOCTOR', 'PATIENT')")
    public ResponseEntity<ApiResponse<DrugSuggestionResponse>> getDrugDetails(
            @RequestParam String drugName) {
        DrugSuggestionResponse details = smartPrescriptionService.getDrugDetails(drugName);
        if (details != null) {
            return ResponseEntity.ok(new ApiResponse<>(true, details));
        } else {
            return ResponseEntity.ok(new ApiResponse<>(false, null));
        }
    }

    /**
     * Kiểm tra tương tác thuốc
     */
    @PostMapping("/check-interactions")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<DrugInteractionResponse>> checkDrugInteractions(
            @RequestBody List<String> drugNames) {
        DrugInteractionResponse result = smartPrescriptionService.checkInteractions(drugNames);
        return ResponseEntity.ok(new ApiResponse<>(true, result));
    }

    /**
     * Tìm thuốc generic thay thế
     */
    @GetMapping("/generic-alternatives")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<List<GenericDrugResponse>>> findGenericAlternatives(
            @RequestParam String drugName) {
        List<GenericDrugResponse> alternatives = smartPrescriptionService.findGenericAlternatives(drugName);
        return ResponseEntity.ok(new ApiResponse<>(true, alternatives));
    }

    /**
     * Gợi ý thuốc theo chẩn đoán
     */
    @GetMapping("/suggest-by-diagnosis")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<List<DrugSuggestionResponse>>> suggestDrugsByDiagnosis(
            @RequestParam String diagnosis) {
        List<DrugSuggestionResponse> suggestions = smartPrescriptionService.suggestDrugsForDiagnosis(diagnosis);
        return ResponseEntity.ok(new ApiResponse<>(true, suggestions));
    }

    /**
     * Phân tích đơn thuốc thông minh
     */
    @PostMapping("/analyze-prescription")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<SmartPrescriptionResponse>> analyzePrescription(
            @RequestBody MedicalRecordRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        // Đảm bảo doctor ID từ JWT
        Long doctorUserId = extractUserId(userDetails);
        request.setDoctorId(doctorUserId);

        SmartPrescriptionResponse analysis = smartPrescriptionService.analyzePrescription(request);
        return ResponseEntity.ok(new ApiResponse<>(analysis.isSuccess(), analysis));
    }

    /**
     * Phân tích lịch sử dùng thuốc của bệnh nhân
     */
    @GetMapping("/analyze-patient-history/{patientId}")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<SmartPrescriptionResponse>> analyzePatientMedicationHistory(
            @PathVariable Long patientId) {
        SmartPrescriptionResponse analysis = smartPrescriptionService.analyzePatientMedicationHistory(patientId);
        return ResponseEntity.ok(new ApiResponse<>(analysis.isSuccess(), analysis));
    }

    /**
     * Lấy gợi ý thuốc nhanh cho các bệnh phổ biến
     */
    @GetMapping("/quick-suggestions")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<List<DrugSuggestionResponse>>> getQuickSuggestions() {
        // Gợi ý một số thuốc phổ biến
        List<String> commonSymptoms = List.of("đau đầu", "sốt", "cảm cúm", "đau bụng", "ho");
        List<DrugSuggestionResponse> quickSuggestions = commonSymptoms.stream()
                .flatMap(symptom -> smartPrescriptionService.suggestDrugsForDiagnosis(symptom).stream())
                .distinct()
                .limit(20)
                .collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(new ApiResponse<>(true, quickSuggestions));
    }

    /**
     * Kiểm tra an toàn thuốc cho bệnh nhân cụ thể
     */
    @PostMapping("/safety-check/{patientId}")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<SmartPrescriptionResponse>> checkDrugSafety(
            @PathVariable Long patientId,
            @RequestBody List<String> drugNames) {

        // Tạo request để kiểm tra
        MedicalRecordRequest request = new MedicalRecordRequest();
        request.setPatientId(patientId);

        List<com.hospital.telemedicine.dto.request.PrescriptionDetailRequest> details = drugNames.stream()
                .map(drugName -> {
                    com.hospital.telemedicine.dto.request.PrescriptionDetailRequest detail =
                            new com.hospital.telemedicine.dto.request.PrescriptionDetailRequest();
                    detail.setMedicationName(drugName);
                    return detail;
                })
                .collect(java.util.stream.Collectors.toList());

        request.setPrescriptionDetails(details);

        SmartPrescriptionResponse safetyCheck = smartPrescriptionService.analyzePrescription(request);
        return ResponseEntity.ok(new ApiResponse<>(safetyCheck.isSuccess(), safetyCheck));
    }

    /**
     * Lấy thống kê về tương tác thuốc trong hệ thống
     */
    @GetMapping("/interaction-stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DrugInteractionStats>> getDrugInteractionStats() {
        // Thống kê đơn giản (có thể mở rộng)
        DrugInteractionStats stats = new DrugInteractionStats();
        stats.setTotalChecks(0L); // Có thể implement counter
        stats.setHighRiskInteractions(0L);
        stats.setModerateRiskInteractions(0L);
        stats.setLowRiskInteractions(0L);

        return ResponseEntity.ok(new ApiResponse<>(true, stats));
    }

    private Long extractUserId(UserDetails userDetails) {
        if (userDetails instanceof com.hospital.telemedicine.security.UserDetailsImpl) {
            return ((com.hospital.telemedicine.security.UserDetailsImpl) userDetails).getId();
        }
        throw new IllegalArgumentException("UserDetails must be an instance of UserDetailsImpl");
    }

    // Inner class for stats
    public static class DrugInteractionStats {
        private Long totalChecks;
        private Long highRiskInteractions;
        private Long moderateRiskInteractions;
        private Long lowRiskInteractions;

        // Getters and setters
        public Long getTotalChecks() { return totalChecks; }
        public void setTotalChecks(Long totalChecks) { this.totalChecks = totalChecks; }

        public Long getHighRiskInteractions() { return highRiskInteractions; }
        public void setHighRiskInteractions(Long highRiskInteractions) { this.highRiskInteractions = highRiskInteractions; }

        public Long getModerateRiskInteractions() { return moderateRiskInteractions; }
        public void setModerateRiskInteractions(Long moderateRiskInteractions) { this.moderateRiskInteractions = moderateRiskInteractions; }

        public Long getLowRiskInteractions() { return lowRiskInteractions; }
        public void setLowRiskInteractions(Long lowRiskInteractions) { this.lowRiskInteractions = lowRiskInteractions; }
    }
}