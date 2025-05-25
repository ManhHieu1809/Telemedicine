package com.hospital.telemedicine.controller;

import com.hospital.telemedicine.dto.request.AppointmentRequest;
import com.hospital.telemedicine.dto.response.AppointmentResponse;
import com.hospital.telemedicine.service.AppointmentService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    // Đặt lịch hẹn mới (chỉ PATIENT)
    @PostMapping
    public ResponseEntity<AppointmentResponse> createAppointment(
            @RequestBody AppointmentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        // Kiểm tra vai trò và lấy userId từ JWT
        String role = extractRole(userDetails);
        if (!"PATIENT".equals(role)) {
            throw new SecurityException("Only patients can create appointments");
        }

        // Lấy userId từ JWT và gán vào request
        Long userId = extractUserId(userDetails);
        System.out.println("JWT User ID: " + userId + ", Role: " + role);
        request.setPatientId(userId); // Đây là userId, KHÔNG phải patientId

        AppointmentResponse response = appointmentService.createAppointment(request);
        return ResponseEntity.ok(response);
    }

    // Xem danh sách lịch hẹn (PATIENT: của mình, DOCTOR: của mình, ADMIN: tất cả)
    @GetMapping
    public ResponseEntity<List<AppointmentResponse>> getAppointments(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = extractUserId(userDetails);
        String role = extractRole(userDetails);
        List<AppointmentResponse> appointments = appointmentService.getAppointments(userId, role);
        return ResponseEntity.ok(appointments);
    }

    // Lấy lịch trống của bác sĩ trong một ngày (chỉ PATIENT)
    @GetMapping("/available-slots")
    public ResponseEntity<List<LocalTime>> getAvailableSlots(
            @RequestParam Long doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal UserDetails userDetails) {
        String role = extractRole(userDetails);
        if (!"PATIENT".equals(role)) {
            throw new SecurityException("Only patients can view available slots");
        }
        List<LocalTime> availableSlots = appointmentService.getAvailableSlots(doctorId, date);
        return ResponseEntity.ok(availableSlots);
    }

    // Cập nhật lịch hẹn (chỉ PATIENT, chỉ PENDING)
    @PutMapping("/{id}")
    public ResponseEntity<AppointmentResponse> updateAppointment(
            @PathVariable Long id,
            @RequestBody AppointmentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        String role = extractRole(userDetails);
        if (!"PATIENT".equals(role)) {
            throw new SecurityException("Only patients can update appointments");
        }
        Long userId = extractUserId(userDetails);
        request.setPatientId(userId); // Gán userId từ JWT

        AppointmentResponse response = appointmentService.updateAppointment(id, request);
        return ResponseEntity.ok(response);
    }

    // Hủy lịch hẹn (PATIENT hoặc DOCTOR)
    @DeleteMapping("/{id}")
    public ResponseEntity<AppointmentResponse> cancelAppointment(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        String role = extractRole(userDetails);
        if (!"PATIENT".equals(role) && !"DOCTOR".equals(role)) {
            throw new SecurityException("Only patients or doctors can cancel appointments");
        }
        AppointmentResponse response = appointmentService.cancelAppointment(id);
        return ResponseEntity.ok(response);
    }

    // Xác nhận lịch hẹn (chỉ DOCTOR, chỉ PENDING)
    @PostMapping("/{id}/confirm")
    public ResponseEntity<AppointmentResponse> confirmAppointment(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        String role = extractRole(userDetails);
        if (!"DOCTOR".equals(role)) {
            throw new SecurityException("Only doctors can confirm appointments");
        }
        AppointmentResponse response = appointmentService.confirmAppointment(id);
        return ResponseEntity.ok(response);
    }

    // Hoàn thành lịch hẹn (chỉ DOCTOR, chỉ CONFIRMED)
    @PostMapping("/{id}/complete")
    public ResponseEntity<AppointmentResponse> completeAppointment(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        String role = extractRole(userDetails);
        if (!"DOCTOR".equals(role)) {
            throw new SecurityException("Only doctors can complete appointments");
        }
        AppointmentResponse response = appointmentService.completeAppointment(id);
        return ResponseEntity.ok(response);
    }

    // Hàm hỗ trợ: Lấy userId từ UserDetailsImpl
    private Long extractUserId(UserDetails userDetails) {
        if (userDetails instanceof com.hospital.telemedicine.security.UserDetailsImpl) {
            return ((com.hospital.telemedicine.security.UserDetailsImpl) userDetails).getId();
        }
        throw new IllegalArgumentException("UserDetails must be an instance of UserDetailsImpl");
    }

    // Hàm hỗ trợ: Lấy role từ UserDetailsImpl
    private String extractRole(UserDetails userDetails) {
        if (userDetails instanceof com.hospital.telemedicine.security.UserDetailsImpl) {
            return ((com.hospital.telemedicine.security.UserDetailsImpl) userDetails).getAuthorities().stream()
                    .map(auth -> auth.getAuthority().replace("ROLE_", ""))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Role not found in authorities"));
        }
        throw new IllegalArgumentException("UserDetails must be an instance of UserDetailsImpl");
    }
}