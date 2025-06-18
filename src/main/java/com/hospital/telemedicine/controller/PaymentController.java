// src/main/java/com/hospital/telemedicine/controller/PaymentController.java
package com.hospital.telemedicine.controller;

import com.hospital.telemedicine.dto.request.PaymentRequest;
import com.hospital.telemedicine.dto.response.ApiResponse;
import com.hospital.telemedicine.dto.response.PaymentResponse;
import com.hospital.telemedicine.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    /**
     * Tạo thanh toán VNPay
     */
    @PostMapping("/vnpay/create")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<PaymentResponse>> createVnpayPayment(
            @RequestBody PaymentRequest request,
            HttpServletRequest httpRequest) {

        // Lấy IP address của client
        String ipAddress = getClientIpAddress(httpRequest);
        request.setIpAddress(ipAddress);

        PaymentResponse response = paymentService.createVnpayPayment(request);
        return ResponseEntity.ok(new ApiResponse<>(response.isSuccess(), response));
    }

    /**
     * Tạo thanh toán MoMo
     */
    @PostMapping("/momo/create")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<PaymentResponse>> createMomoPayment(
            @RequestBody PaymentRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = getClientIpAddress(httpRequest);
        request.setIpAddress(ipAddress);

        PaymentResponse response = paymentService.createMomoPayment(request);
        return ResponseEntity.ok(new ApiResponse<>(response.isSuccess(), response));
    }

    /**
     * Callback từ VNPay (GET method cho return URL)
     */
    @GetMapping("/vnpay/callback")
    public ResponseEntity<String> vnpayCallback(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();

        // Lấy tất cả parameters từ request
        request.getParameterMap().forEach((key, value) -> {
            if (value.length > 0) {
                params.put(key, value[0]);
            }
        });

        PaymentResponse response = paymentService.handleVnpayCallback(params);

        // Redirect về frontend với kết quả
        String redirectUrl = "http://localhost:3000/payment-result?";
        redirectUrl += "success=" + response.isSuccess();
        redirectUrl += "&paymentId=" + response.getPaymentId();
        redirectUrl += "&message=" + response.getMessage();

        return ResponseEntity.status(302)
                .header("Location", redirectUrl)
                .build();
    }

    /**
     * IPN từ VNPay (POST method cho notification)
     */
    @PostMapping("/vnpay/ipn")
    public ResponseEntity<Map<String, String>> vnpayIpn(@RequestParam Map<String, String> params) {
        log.info("Received VNPay IPN: {}", params);

        PaymentResponse response = paymentService.handleVnpayCallback(params);

        Map<String, String> result = new HashMap<>();
        if (response.isSuccess()) {
            result.put("RspCode", "00");
            result.put("Message", "Confirm Success");
        } else {
            result.put("RspCode", "99");
            result.put("Message", "Confirm Fail");
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Callback từ MoMo (return URL)
     */
    @GetMapping("/momo/return")
    public ResponseEntity<String> momoReturn(HttpServletRequest request) {
        Map<String, Object> params = new HashMap<>();

        // Lấy parameters từ MoMo
        String orderId = request.getParameter("orderId");
        String resultCode = request.getParameter("resultCode");
        String message = request.getParameter("message");

        // Redirect về frontend với kết quả
        String redirectUrl = "http://localhost:3000/payment-result?";
        redirectUrl += "success=" + ("0".equals(resultCode));
        redirectUrl += "&orderId=" + orderId;
        redirectUrl += "&message=" + message;

        return ResponseEntity.status(302)
                .header("Location", redirectUrl)
                .build();
    }

    /**
     * IPN từ MoMo (notification URL)
     */
    @PostMapping("/momo/callback")
    public ResponseEntity<Map<String, Object>> momoCallback(@RequestBody Map<String, Object> params) {
        log.info("Received MoMo IPN: {}", params);

        PaymentResponse response = paymentService.handleMomoCallback(params);

        Map<String, Object> result = new HashMap<>();
        if (response.isSuccess()) {
            result.put("resultCode", 0);
            result.put("message", "Success");
        } else {
            result.put("resultCode", 1);
            result.put("message", "Failed");
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Lấy trạng thái thanh toán
     */
    @GetMapping("/{paymentId}/status")
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentStatus(@PathVariable Long paymentId) {
        PaymentResponse response = paymentService.getPaymentStatus(paymentId);
        return ResponseEntity.ok(new ApiResponse<>(response.isSuccess(), response));
    }

    /**
     * Lấy danh sách thanh toán của bệnh nhân
     */
    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getPatientPayments(@PathVariable Long patientId) {
        List<PaymentResponse> responses = paymentService.getPatientPayments(patientId);
        return ResponseEntity.ok(new ApiResponse<>(true, responses));
    }

    /**
     * Lấy IP address của client
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}