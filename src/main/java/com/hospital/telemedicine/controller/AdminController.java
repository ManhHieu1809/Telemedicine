package com.hospital.telemedicine.controller;

import com.hospital.telemedicine.dto.*;
import com.hospital.telemedicine.dto.request.*;
import com.hospital.telemedicine.dto.response.*;
import com.hospital.telemedicine.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    @Autowired
    private UserService userService;



    // Quản lý người dùng: Bệnh nhân
    @PostMapping("/users/patients")
    public ResponseEntity<Void> createPatient(@RequestBody CreatePatientRequest request) {
        userService.createPatient(request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/users/patients/{patientId}")
    public ResponseEntity<Void> updatePatient(@PathVariable Long patientId,
                                              @RequestBody UpdatePatientRequest request) {
        userService.updatePatient(patientId, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/users/patients/{patientId}")
    public ResponseEntity<Void> deletePatient(@PathVariable Long patientId) {
        userService.deletePatient(patientId);
        return ResponseEntity.ok().build();
    }

    // Quản lý người dùng: Bác sĩ
    @PutMapping("/users/doctors/{doctorId}")
    public ResponseEntity<Void> updateDoctor(@PathVariable Long doctorId,
                                             @RequestBody UpdateDoctorRequest request) {
        userService.updateDoctor(doctorId, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/users/doctors/{doctorId}")
    public ResponseEntity<Void> deleteDoctor(@PathVariable Long doctorId) {
        userService.deleteDoctor(doctorId);
        return ResponseEntity.ok().build();
    }

    // Giám sát hệ thống: Báo cáo
    @GetMapping("/reports")
    public ResponseEntity<SystemReportResponse> getSystemReport() {
        SystemReportResponse report = userService.getSystemReport();
        return ResponseEntity.ok(report);
    }

    // Giám sát hệ thống: Hoạt động người dùng
    @GetMapping("/user-activities")
    public ResponseEntity<List<UserActivityResponse>> getUserActivities() {
        List<UserActivityResponse> activities = userService.getUserActivities();
        return ResponseEntity.ok(activities);
    }

    // Kiểm duyệt nội dung: Đánh giá
    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long reviewId) {
        userService.deleteReview(reviewId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/reviews/{reviewId}")
    public ResponseEntity<Void> updateReview(@PathVariable Long reviewId,
                                             @RequestBody UpdateReviewRequest request) {
        userService.updateReview(reviewId, request);
        return ResponseEntity.ok().build();
    }

    // Kiểm duyệt nội dung: Tin nhắn
    @DeleteMapping("/chat-messages/{messageId}")
    public ResponseEntity<Void> deleteChatMessage(@PathVariable Long messageId) {
        userService.deleteMessage(messageId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/chat-messages/{messageId}")
    public ResponseEntity<Void> updateChatMessage(@PathVariable Long messageId,
                                                  @RequestBody UpdateMessageRequest request) {
        userService.updateMessage(messageId, request);
        return ResponseEntity.ok().build();
    }

    // Lấy tất cả người dùng
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(new ApiResponse<>(true, users));
    }

    // Lấy danh sách bệnh nhân
    @GetMapping("/users/patients")
    public ResponseEntity<ApiResponse<List<PatientResponse>>> getAllPatients() {
        List<PatientResponse> patients = userService.getAllPatients();
        return ResponseEntity.ok(new ApiResponse<>(true, patients));
    }

    // Lấy dashboard statistics
    @GetMapping("/dashboard/stats")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getDashboardStats() {
        DashboardStatsResponse stats = userService.getDashboardStats();
        return ResponseEntity.ok(new ApiResponse<>(true, stats));
    }

}