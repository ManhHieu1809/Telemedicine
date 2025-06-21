
package com.hospital.telemedicine.controller;

import com.hospital.telemedicine.dto.response.ApiResponse;
import com.hospital.telemedicine.dto.response.PaymentResponse;
import com.hospital.telemedicine.dto.response.PaymentStatisticsResponse;
import com.hospital.telemedicine.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/payments")
@PreAuthorize("hasRole('ADMIN')")
public class PaymentAdminController {

    @Autowired
    private PaymentService paymentService;

    /**
     * Lấy thống kê thanh toán tổng quan
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<PaymentStatisticsResponse>> getPaymentStatistics() {
        PaymentStatisticsResponse statistics = paymentService.getPaymentStatistics();
        return ResponseEntity.ok(new ApiResponse<>(true, statistics));
    }

    /**
     * Lấy tất cả thanh toán với phân trang
     */
    // Trong PaymentAdminController.java
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {

        Page<PaymentResponse> paymentsPage = paymentService.getAllPaymentsWithPagination(page, size, status);

        Map<String, Object> response = new HashMap<>();
        response.put("content", paymentsPage.getContent());
        response.put("totalPages", paymentsPage.getTotalPages());
        response.put("totalElements", paymentsPage.getTotalElements());
        response.put("number", paymentsPage.getNumber());
        response.put("size", paymentsPage.getSize());
        response.put("first", paymentsPage.isFirst());
        response.put("last", paymentsPage.isLast());

        return ResponseEntity.ok(new ApiResponse<>(true, response));
    }

    /**
     * Lấy thanh toán theo khoảng thời gian
     */
    @GetMapping("/by-date-range")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getPaymentsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        List<PaymentResponse> payments = paymentService.getPaymentsByDateRange(startDateTime, endDateTime);
        return ResponseEntity.ok(new ApiResponse<>(true, payments));
    }

    /**
     * Lấy thanh toán của bác sĩ
     */
    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getDoctorPayments(@PathVariable Long doctorId) {
        List<PaymentResponse> payments = paymentService.getDoctorPayments(doctorId);
        return ResponseEntity.ok(new ApiResponse<>(true, payments));
    }

    /**
     * Hoàn tiền
     */
    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<ApiResponse<PaymentResponse>> refundPayment(
            @PathVariable Long paymentId,
            @RequestParam String reason) {
        PaymentResponse response = paymentService.refundPayment(paymentId, reason);
        return ResponseEntity.ok(new ApiResponse<>(response.isSuccess(), response));
    }

    /**
     * Kiểm tra trạng thái thanh toán từ gateway
     */
    @PostMapping("/{paymentId}/check-status")
    public ResponseEntity<ApiResponse<PaymentResponse>> checkPaymentStatus(@PathVariable Long paymentId) {
        PaymentResponse response = paymentService.checkPaymentStatusFromGateway(paymentId);
        return ResponseEntity.ok(new ApiResponse<>(response.isSuccess(), response));
    }

    /**
     * Lấy thanh toán thất bại
     */
    @GetMapping("/failed")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getFailedPayments(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : LocalDateTime.now().minusDays(30);
        LocalDateTime endDateTime = endDate != null ? endDate.atTime(23, 59, 59) : LocalDateTime.now();

        List<PaymentResponse> failedPayments = paymentService.getFailedPayments(startDateTime, endDateTime);
        return ResponseEntity.ok(new ApiResponse<>(true, failedPayments));
    }

    /**
     * Lấy thanh toán chờ xử lý quá lâu
     */
    @GetMapping("/stale-pending")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getStalePendingPayments() {
        List<PaymentResponse> stalePayments = paymentService.getStalePendingPayments();
        return ResponseEntity.ok(new ApiResponse<>(true, stalePayments));
    }

    /**
     * Export báo cáo thanh toán
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportPaymentReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "excel") String format) {

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        byte[] reportData = paymentService.exportPaymentReport(startDateTime, endDateTime, format);

        String fileName = "payment_report_" + startDate + "_to_" + endDate + "." +
                (format.equals("pdf") ? "pdf" : "xlsx");

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=" + fileName)
                .header("Content-Type", format.equals("pdf") ?
                        "application/pdf" : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .body(reportData);
    }

    /**
     * Thống kê doanh thu theo tháng
     */
    @GetMapping("/revenue/monthly")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMonthlyRevenue(
            @RequestParam int year) {
        Map<String, Object> monthlyRevenue = paymentService.getMonthlyRevenue(year);
        return ResponseEntity.ok(new ApiResponse<>(true, monthlyRevenue));
    }

    /**
     * Top bệnh nhân có nhiều thanh toán nhất
     */
    @GetMapping("/top-patients")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTopPayingPatients(
            @RequestParam(defaultValue = "10") int limit) {
        List<Map<String, Object>> topPatients = paymentService.getTopPayingPatients(limit);
        return ResponseEntity.ok(new ApiResponse<>(true, topPatients));
    }

    /**
     * Top bác sĩ có doanh thu cao nhất
     */
    @GetMapping("/top-doctors")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTopRevenueGeneratingDoctors(
            @RequestParam(defaultValue = "10") int limit) {
        List<Map<String, Object>> topDoctors = paymentService.getTopRevenueGeneratingDoctors(limit);
        return ResponseEntity.ok(new ApiResponse<>(true, topDoctors));
    }

    /**
     * Thống kê chi tiết theo ngày
     */
    @GetMapping("/daily-stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDailyStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Map<String, Object> dailyStats = paymentService.getDailyStatistics(date);
        return ResponseEntity.ok(new ApiResponse<>(true, dailyStats));
    }

    /**
     * Thống kê theo phương thức thanh toán
     */
    @GetMapping("/method-stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPaymentMethodStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        Map<String, Object> methodStats = paymentService.getPaymentMethodStatistics(startDateTime, endDateTime);
        return ResponseEntity.ok(new ApiResponse<>(true, methodStats));
    }

    /**
     * Dashboard overview
     */
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardData() {
        Map<String, Object> dashboardData = paymentService.getDashboardOverview();
        return ResponseEntity.ok(new ApiResponse<>(true, dashboardData));
    }
}