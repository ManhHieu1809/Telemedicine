
package com.hospital.telemedicine.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.telemedicine.config.PaymentConfig;
import com.hospital.telemedicine.dto.request.PaymentRequest;
import com.hospital.telemedicine.dto.response.PaymentResponse;
import com.hospital.telemedicine.dto.response.PaymentStatisticsResponse;
import com.hospital.telemedicine.entity.*;
import com.hospital.telemedicine.repository.*;
import com.hospital.telemedicine.util.VnpayUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Font;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import java.io.ByteArrayOutputStream;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PaymentConfig paymentConfig;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Tạo thanh toán VNPay
     */
    public PaymentResponse createVnpayPayment(PaymentRequest request) {
        try {
            log.info("Creating VNPay payment for appointment: {}", request.getAppointmentId());

            // Validate và lấy thông tin appointment
            Appointment appointment = appointmentRepository.findById(request.getAppointmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

            if (!appointment.getStatus().equals(Appointment.Status.CONFIRMED)) {
                throw new IllegalStateException("Only confirmed appointments can be paid");
            }

            // Tạo payment record
            Payment payment = createPaymentRecord(appointment, request.getAmount(), Payment.PaymentMethod.VNPAY);

            // Tạo VNPay URL
            String vnpayUrl = buildVnpayUrl(payment, request);

            return PaymentResponse.builder()
                    .success(true)
                    .paymentId(payment.getId())
                    .paymentUrl(vnpayUrl)
                    .paymentMethod(Payment.PaymentMethod.VNPAY)
                    .amount(payment.getAmount())
                    .message("VNPay payment URL created successfully")
                    .build();

        } catch (Exception e) {
            log.error("Error creating VNPay payment: ", e);
            return PaymentResponse.builder()
                    .success(false)
                    .message("Failed to create VNPay payment: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Tạo thanh toán MoMo
     */
    public PaymentResponse createMomoPayment(PaymentRequest request) {
        try {
            log.info("Creating MoMo payment for appointment: {}", request.getAppointmentId());

            // Validate và lấy thông tin appointment
            Appointment appointment = appointmentRepository.findById(request.getAppointmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

            if (!appointment.getStatus().equals(Appointment.Status.CONFIRMED)) {
                throw new IllegalStateException("Only confirmed appointments can be paid");
            }

            // Tạo payment record
            Payment payment = createPaymentRecord(appointment, request.getAmount(), Payment.PaymentMethod.MOMO);

            // Gọi MoMo API
            String momoPayUrl = callMomoAPI(payment, request);

            return PaymentResponse.builder()
                    .success(true)
                    .paymentId(payment.getId())
                    .paymentUrl(momoPayUrl)
                    .paymentMethod(Payment.PaymentMethod.MOMO)
                    .amount(payment.getAmount())
                    .message("MoMo payment URL created successfully")
                    .build();

        } catch (Exception e) {
            log.error("Error creating MoMo payment: ", e);
            return PaymentResponse.builder()
                    .success(false)
                    .message("Failed to create MoMo payment: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Xử lý callback từ VNPay
     */
    public PaymentResponse handleVnpayCallback(Map<String, String> params) {
        try {
            log.info("Processing VNPay callback: {}", params);

            // Verify signature
            if (!VnpayUtil.verifySignature(params, paymentConfig.getVnpay().getSecretKey())) {
                log.error("Invalid VNPay signature");
                return PaymentResponse.builder()
                        .success(false)
                        .message("Invalid signature")
                        .build();
            }

            String orderId = params.get("vnp_TxnRef");
            String responseCode = params.get("vnp_ResponseCode");
            String transactionNo = params.get("vnp_TransactionNo");
            String amount = params.get("vnp_Amount");

            // Tìm payment record
            Payment payment = paymentRepository.findById(Long.parseLong(orderId))
                    .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

            // Cập nhật trạng thái payment
            if ("00".equals(responseCode)) {
                payment.setStatus(Payment.PaymentStatus.COMPLETED);
                log.info("VNPay payment completed successfully: {}", orderId);

                // Cập nhật appointment status nếu cần
                updateAppointmentAfterPayment(payment.getAppointment());

            } else {
                payment.setStatus(Payment.PaymentStatus.FAILED);
                log.warn("VNPay payment failed with code: {}", responseCode);
            }

            paymentRepository.save(payment);

            return PaymentResponse.builder()
                    .success("00".equals(responseCode))
                    .paymentId(payment.getId())
                    .transactionId(transactionNo)
                    .amount(payment.getAmount())
                    .status(payment.getStatus())
                    .message("Payment processed")
                    .build();

        } catch (Exception e) {
            log.error("Error processing VNPay callback: ", e);
            return PaymentResponse.builder()
                    .success(false)
                    .message("Failed to process VNPay callback: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Xử lý callback từ MoMo
     */
    public PaymentResponse handleMomoCallback(Map<String, Object> params) {
        try {
            log.info("Processing MoMo callback: {}", params);

            // Verify signature
            if (!verifyMomoSignature(params)) {
                log.error("Invalid MoMo signature");
                return PaymentResponse.builder()
                        .success(false)
                        .message("Invalid signature")
                        .build();
            }

            String orderId = (String) params.get("orderId");
            Integer resultCode = (Integer) params.get("resultCode");
            String transId = (String) params.get("transId");
            Long amount = (Long) params.get("amount");

            // Extract payment ID from orderId
            String paymentIdStr = orderId.replace("PAY", "");
            Payment payment = paymentRepository.findById(Long.parseLong(paymentIdStr))
                    .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

            // Cập nhật trạng thái payment
            if (resultCode == 0) {
                payment.setStatus(Payment.PaymentStatus.COMPLETED);
                log.info("MoMo payment completed successfully: {}", orderId);

                // Cập nhật appointment status nếu cần
                updateAppointmentAfterPayment(payment.getAppointment());

            } else {
                payment.setStatus(Payment.PaymentStatus.FAILED);
                log.warn("MoMo payment failed with code: {}", resultCode);
            }

            paymentRepository.save(payment);

            return PaymentResponse.builder()
                    .success(resultCode == 0)
                    .paymentId(payment.getId())
                    .transactionId(transId)
                    .amount(payment.getAmount())
                    .status(payment.getStatus())
                    .message("Payment processed")
                    .build();

        } catch (Exception e) {
            log.error("Error processing MoMo callback: ", e);
            return PaymentResponse.builder()
                    .success(false)
                    .message("Failed to process MoMo callback: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Lấy thông tin payment
     */
    public PaymentResponse getPaymentStatus(Long paymentId) {
        try {
            Payment payment = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

            return PaymentResponse.builder()
                    .success(true)
                    .paymentId(payment.getId())
                    .amount(payment.getAmount())
                    .status(payment.getStatus())
                    .paymentMethod(payment.getPaymentMethod())
                    .createdAt(payment.getCreatedAt())
                    .appointmentId(payment.getAppointment().getId())
                    .patientName(payment.getPatient().getFullName())
                    .doctorName(payment.getDoctor().getFullName())
                    .message("Payment details retrieved successfully")
                    .build();

        } catch (Exception e) {
            log.error("Error getting payment status: ", e);
            return PaymentResponse.builder()
                    .success(false)
                    .message("Failed to get payment status: " + e.getMessage())
                    .build();
        }
    }

    // Helper Methods

    private Payment createPaymentRecord(Appointment appointment, BigDecimal amount, Payment.PaymentMethod method) {
        Payment payment = new Payment();
        payment.setAppointment(appointment);
        payment.setPatient(appointment.getPatient());
        payment.setDoctor(appointment.getDoctor());
        payment.setAmount(amount);
        payment.setPaymentMethod(method);
        payment.setStatus(Payment.PaymentStatus.PENDING);
        payment.setCreatedAt(LocalDateTime.now());

        return paymentRepository.save(payment);
    }

    private String buildVnpayUrl(Payment payment, PaymentRequest request) throws Exception {
        Map<String, String> vnpParams = new TreeMap<>();

        vnpParams.put("vnp_Version", paymentConfig.getVnpay().getVersion());
        vnpParams.put("vnp_Command", paymentConfig.getVnpay().getCommand());
        vnpParams.put("vnp_TmnCode", paymentConfig.getVnpay().getTmnCode());
        vnpParams.put("vnp_Amount", String.valueOf(payment.getAmount().multiply(new BigDecimal(100)).longValue()));
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", payment.getId().toString());
        vnpParams.put("vnp_OrderInfo", "Thanh toan lich hen: " + payment.getAppointment().getId());
        vnpParams.put("vnp_OrderType", paymentConfig.getVnpay().getOrderType());
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", paymentConfig.getVnpay().getReturnUrl());
        vnpParams.put("vnp_IpAddr", request.getIpAddress() != null ? request.getIpAddress() : "127.0.0.1");
        vnpParams.put("vnp_CreateDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));

        // Build hash data
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        for (Map.Entry<String, String> entry : vnpParams.entrySet()) {
            hashData.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8.toString()));
            hashData.append('=');
            hashData.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.toString()));
            hashData.append('&');

            query.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8.toString()));
            query.append('=');
            query.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.toString()));
            query.append('&');
        }

        hashData.setLength(hashData.length() - 1);
        query.setLength(query.length() - 1);

        String vnpSecureHash = VnpayUtil.hmacSHA512(paymentConfig.getVnpay().getSecretKey(), hashData.toString());
        query.append("&vnp_SecureHash=").append(vnpSecureHash);

        return paymentConfig.getVnpay().getPayUrl() + "?" + query.toString();
    }

    private String callMomoAPI(Payment payment, PaymentRequest request) throws Exception {
        Map<String, Object> momoParams = new HashMap<>();

        String orderId = "PAY" + payment.getId();
        String requestId = "REQ" + System.currentTimeMillis();

        momoParams.put("partnerCode", paymentConfig.getMomo().getPartnerCode());
        momoParams.put("partnerName", "Hospital Telemedicine");
        momoParams.put("storeId", "HospitalStore");
        momoParams.put("requestId", requestId);
        momoParams.put("amount", payment.getAmount().longValue());
        momoParams.put("orderId", orderId);
        momoParams.put("orderInfo", "Thanh toan lich hen: " + payment.getAppointment().getId());
        momoParams.put("redirectUrl", paymentConfig.getMomo().getReturnUrl());
        momoParams.put("ipnUrl", paymentConfig.getMomo().getNotifyUrl());
        momoParams.put("requestType", paymentConfig.getMomo().getRequestType());
        momoParams.put("extraData", "");
        momoParams.put("lang", "vi");

        // Create signature
        String rawSignature = "accessKey=" + paymentConfig.getMomo().getAccessKey() +
                "&amount=" + payment.getAmount().longValue() +
                "&extraData=" +
                "&ipnUrl=" + paymentConfig.getMomo().getNotifyUrl() +
                "&orderId=" + orderId +
                "&orderInfo=" + momoParams.get("orderInfo") +
                "&partnerCode=" + paymentConfig.getMomo().getPartnerCode() +
                "&redirectUrl=" + paymentConfig.getMomo().getReturnUrl() +
                "&requestId=" + requestId +
                "&requestType=" + paymentConfig.getMomo().getRequestType();

        String signature = hmacSHA256(rawSignature, paymentConfig.getMomo().getSecretKey());
        momoParams.put("signature", signature);

        // Call MoMo API
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(momoParams, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(
                paymentConfig.getMomo().getPaymentUrl(), entity, Map.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            Map<String, Object> responseBody = response.getBody();
            Integer resultCode = (Integer) responseBody.get("resultCode");

            if (resultCode == 0) {
                return (String) responseBody.get("payUrl");
            } else {
                throw new RuntimeException("MoMo API error: " + responseBody.get("message"));
            }
        } else {
            throw new RuntimeException("Failed to call MoMo API");
        }
    }

    private boolean verifyMomoSignature(Map<String, Object> params) {
        try {
            String receivedSignature = (String) params.get("signature");

            String rawSignature = "accessKey=" + paymentConfig.getMomo().getAccessKey() +
                    "&amount=" + params.get("amount") +
                    "&extraData=" + params.get("extraData") +
                    "&message=" + params.get("message") +
                    "&orderId=" + params.get("orderId") +
                    "&orderInfo=" + params.get("orderInfo") +
                    "&orderType=" + params.get("orderType") +
                    "&partnerCode=" + params.get("partnerCode") +
                    "&payType=" + params.get("payType") +
                    "&requestId=" + params.get("requestId") +
                    "&responseTime=" + params.get("responseTime") +
                    "&resultCode=" + params.get("resultCode") +
                    "&transId=" + params.get("transId");

            String calculatedSignature = hmacSHA256(rawSignature, paymentConfig.getMomo().getSecretKey());

            return calculatedSignature.equals(receivedSignature);
        } catch (Exception e) {
            log.error("Error verifying MoMo signature: ", e);
            return false;
        }
    }

    private void updateAppointmentAfterPayment(Appointment appointment) {
        // Có thể cập nhật trạng thái appointment sau khi thanh toán thành công
        // Ví dụ: appointment.setStatus(Appointment.Status.PAID);
        // appointmentRepository.save(appointment);
    }

    private String hmacSHA256(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(data.getBytes());

        StringBuilder result = new StringBuilder();
        for (byte b : hash) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    public List<PaymentResponse> getPatientPayments(Long patientId) {
        try {
            Patient patient = patientRepository.findById(patientId)
                    .orElseThrow(() -> new IllegalArgumentException("Patient not found"));

            List<Payment> payments = paymentRepository.findByPatientIdOrderByCreatedAtDesc(patientId);

            return payments.stream()
                    .map(this::mapToPaymentResponse)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting patient payments: ", e);
            return Collections.emptyList();
        }
    }

    /**
     * Lấy danh sách thanh toán của bác sĩ
     */
    public List<PaymentResponse> getDoctorPayments(Long doctorId) {
        try {
            Doctor doctor = doctorRepository.findById(doctorId)
                    .orElseThrow(() -> new IllegalArgumentException("Doctor not found"));

            List<Payment> payments = paymentRepository.findByDoctorIdOrderByCreatedAtDesc(doctorId);

            return payments.stream()
                    .map(this::mapToPaymentResponse)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting doctor payments: ", e);
            return Collections.emptyList();
        }
    }

    /**
     * Thống kê thanh toán
     */
    public PaymentStatisticsResponse getPaymentStatistics() {
        try {
            // Tổng số thanh toán
            long totalPayments = paymentRepository.count();

            // Thanh toán thành công
            long completedPayments = paymentRepository.countByStatus(Payment.PaymentStatus.COMPLETED);

            // Thanh toán thất bại
            long failedPayments = paymentRepository.countByStatus(Payment.PaymentStatus.FAILED);

            // Thanh toán đang chờ
            long pendingPayments = paymentRepository.countByStatus(Payment.PaymentStatus.PENDING);

            // Tổng doanh thu
            BigDecimal totalRevenue = paymentRepository.getTotalRevenue();
            if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;

            // Doanh thu theo phương thức thanh toán
            Map<Payment.PaymentMethod, BigDecimal> revenueByMethod = new HashMap<>();
            for (Payment.PaymentMethod method : Payment.PaymentMethod.values()) {
                BigDecimal revenue = paymentRepository.getRevenueByPaymentMethod(method);
                revenueByMethod.put(method, revenue != null ? revenue : BigDecimal.ZERO);
            }

            return PaymentStatisticsResponse.builder()
                    .totalPayments(totalPayments)
                    .completedPayments(completedPayments)
                    .failedPayments(failedPayments)
                    .pendingPayments(pendingPayments)
                    .totalRevenue(totalRevenue)
                    .revenueByMethod(revenueByMethod)
                    .successRate(totalPayments > 0 ? (double) completedPayments / totalPayments * 100 : 0)
                    .build();

        } catch (Exception e) {
            log.error("Error getting payment statistics: ", e);
            return PaymentStatisticsResponse.builder()
                    .totalPayments(0)
                    .completedPayments(0)
                    .failedPayments(0)
                    .pendingPayments(0)
                    .totalRevenue(BigDecimal.ZERO)
                    .revenueByMethod(new HashMap<>())
                    .successRate(0)
                    .build();
        }
    }

    /**
     * Hoàn tiền (refund)
     */
    public PaymentResponse refundPayment(Long paymentId, String reason) {
        try {
            Payment payment = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

            if (!payment.getStatus().equals(Payment.PaymentStatus.COMPLETED)) {
                throw new IllegalStateException("Only completed payments can be refunded");
            }

            // Tạo record hoàn tiền
            Payment refundPayment = new Payment();
            refundPayment.setAppointment(payment.getAppointment());
            refundPayment.setPatient(payment.getPatient());
            refundPayment.setDoctor(payment.getDoctor());
            refundPayment.setAmount(payment.getAmount().negate()); // Số âm để đánh dấu hoàn tiền
            refundPayment.setPaymentMethod(payment.getPaymentMethod());
            refundPayment.setStatus(Payment.PaymentStatus.COMPLETED);
            refundPayment.setCreatedAt(LocalDateTime.now());

            Payment savedRefund = paymentRepository.save(refundPayment);

            // Có thể gọi API hoàn tiền tự động ở đây
            // processAutomaticRefund(payment, reason);

            return PaymentResponse.builder()
                    .success(true)
                    .paymentId(savedRefund.getId())
                    .amount(savedRefund.getAmount())
                    .status(savedRefund.getStatus())
                    .message("Refund processed successfully")
                    .build();

        } catch (Exception e) {
            log.error("Error processing refund: ", e);
            return PaymentResponse.builder()
                    .success(false)
                    .message("Failed to process refund: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Kiểm tra trạng thái thanh toán từ gateway
     */
    public PaymentResponse checkPaymentStatusFromGateway(Long paymentId) {
        try {
            Payment payment = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

            if (payment.getPaymentMethod() == Payment.PaymentMethod.VNPAY) {
                return checkVnpayPaymentStatus(payment);
            } else if (payment.getPaymentMethod() == Payment.PaymentMethod.MOMO) {
                return checkMomoPaymentStatus(payment);
            }

            return PaymentResponse.builder()
                    .success(false)
                    .message("Payment method not supported for status check")
                    .build();

        } catch (Exception e) {
            log.error("Error checking payment status from gateway: ", e);
            return PaymentResponse.builder()
                    .success(false)
                    .message("Failed to check payment status: " + e.getMessage())
                    .build();
        }
    }

    private PaymentResponse checkVnpayPaymentStatus(Payment payment) {
        // Implementation để kiểm tra trạng thái từ VNPay
        // Có thể gọi VNPay Query API
        return mapToPaymentResponse(payment);
    }

    private PaymentResponse checkMomoPaymentStatus(Payment payment) {
        // Implementation để kiểm tra trạng thái từ MoMo
        // Có thể gọi MoMo Query API
        return mapToPaymentResponse(payment);
    }

    private PaymentResponse mapToPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
                .success(true)
                .paymentId(payment.getId())
                .appointmentId(payment.getAppointment().getId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .createdAt(payment.getCreatedAt())
                .patientName(payment.getPatient().getFullName())
                .doctorName(payment.getDoctor().getFullName())
                .message("Payment details retrieved successfully")
                .build();
    }

    /**
     * Lấy tất cả thanh toán với phân trang
     */
    public List<PaymentResponse> getAllPaymentsWithPagination(int page, int size, String status) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<Payment> paymentsPage;

            if (status != null && !status.equals("all")) {
                Payment.PaymentStatus paymentStatus = Payment.PaymentStatus.valueOf(status.toUpperCase());
                paymentsPage = paymentRepository.findByStatus(paymentStatus, pageable);
            } else {
                paymentsPage = paymentRepository.findAll(pageable);
            }

            return paymentsPage.getContent().stream()
                    .map(this::mapToPaymentResponse)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting payments with pagination: ", e);
            return Collections.emptyList();
        }
    }

    /**
     * Lấy thanh toán theo khoảng thời gian
     */
   public List<PaymentResponse> getPaymentsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
       try {
           // Use findPaymentsByDateRange instead of getRevenueBetweenDates
           List<Payment> payments = paymentRepository.findPaymentsByDateRange(startDate, endDate);
           return payments.stream()
                   .map(this::mapToPaymentResponse)
                   .collect(Collectors.toList());

       } catch (Exception e) {
           log.error("Error getting payments by date range: ", e);
           return Collections.emptyList();
       }
   }

    /**
     * Lấy thanh toán thất bại
     */
    public List<PaymentResponse> getFailedPayments(LocalDateTime startDate, LocalDateTime endDate) {
        try {
            List<Payment> failedPayments = paymentRepository.findFailedPaymentsBetweenDates(startDate, endDate);
            return failedPayments.stream()
                    .map(this::mapToPaymentResponse)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting failed payments: ", e);
            return Collections.emptyList();
        }
    }

    /**
     * Lấy thanh toán chờ xử lý quá lâu
     */
    public List<PaymentResponse> getStalePendingPayments() {
        try {
            // Lấy thanh toán pending quá 30 phút
            LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(30);
            List<Payment> stalePayments = paymentRepository.findStalePendingPayments(cutoffTime);

            return stalePayments.stream()
                    .map(this::mapToPaymentResponse)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting stale pending payments: ", e);
            return Collections.emptyList();
        }
    }

    /**
     * Export báo cáo thanh toán
     */
    public byte[] exportPaymentReport(LocalDateTime startDate, LocalDateTime endDate, String format) {
        try {
            List<Payment> payments = paymentRepository.findPaymentsByDateRange(startDate, endDate);

            if ("pdf".equalsIgnoreCase(format)) {
                return generatePdfReport(payments, startDate, endDate);
            } else {
                return generateExcelReport(payments, startDate, endDate);
            }

        } catch (Exception e) {
            log.error("Error exporting payment report: ", e);
            return new byte[0];
        }
    }

    /**
     * Thống kê doanh thu theo tháng
     */
    public Map<String, Object> getMonthlyRevenue(int year) {
        try {
            Map<String, Object> result = new HashMap<>();
            List<Map<String, Object>> monthlyData = new ArrayList<>();

            for (int month = 1; month <= 12; month++) {
                LocalDateTime startDate = LocalDateTime.of(year, month, 1, 0, 0);
                LocalDateTime endDate = startDate.plusMonths(1).minusSeconds(1);

                List<Payment> payments = paymentRepository.findPaymentsByMonth(year, month);

                BigDecimal monthlyRevenue = payments.stream()
                        .filter(p -> p.getStatus() == Payment.PaymentStatus.COMPLETED)
                        .map(Payment::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                long monthlyCount = payments.stream()
                        .filter(p -> p.getStatus() == Payment.PaymentStatus.COMPLETED)
                        .count();

                Map<String, Object> monthData = new HashMap<>();
                monthData.put("month", month);
                monthData.put("monthName", getMonthName(month));
                monthData.put("revenue", monthlyRevenue);
                monthData.put("count", monthlyCount);
                monthData.put("averageAmount", monthlyCount > 0 ?
                        monthlyRevenue.divide(BigDecimal.valueOf(monthlyCount), 2, BigDecimal.ROUND_HALF_UP) :
                        BigDecimal.ZERO);

                monthlyData.add(monthData);
            }

            // Tính tổng năm
            BigDecimal yearlyRevenue = monthlyData.stream()
                    .map(m -> (BigDecimal) m.get("revenue"))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            long yearlyCount = monthlyData.stream()
                    .mapToLong(m -> (Long) m.get("count"))
                    .sum();

            result.put("year", year);
            result.put("monthlyData", monthlyData);
            result.put("yearlyRevenue", yearlyRevenue);
            result.put("yearlyCount", yearlyCount);
            result.put("yearlyAverage", yearlyCount > 0 ?
                    yearlyRevenue.divide(BigDecimal.valueOf(yearlyCount), 2, BigDecimal.ROUND_HALF_UP) :
                    BigDecimal.ZERO);

            return result;

        } catch (Exception e) {
            log.error("Error getting monthly revenue: ", e);
            return new HashMap<>();
        }
    }

    /**
     * Top bệnh nhân có nhiều thanh toán nhất
     */
    public List<Map<String, Object>> getTopPayingPatients(int limit) {
        try {
            List<Object[]> rawData = paymentRepository.findTopPayingPatients();

            return rawData.stream()
                    .limit(limit)
                    .map(row -> {
                        Long patientId = (Long) row[0];
                        Long paymentCount = (Long) row[1];

                        // Lấy thông tin bệnh nhân
                        Optional<Patient> patientOpt = patientRepository.findById(patientId);

                        Map<String, Object> result = new HashMap<>();
                        if (patientOpt.isPresent()) {
                            Patient patient = patientOpt.get();
                            result.put("patientId", patientId);
                            result.put("patientName", patient.getFullName());
                            result.put("phone", patient.getPhone());
                            result.put("paymentCount", paymentCount);

                            // Tính tổng tiền đã thanh toán
                            BigDecimal totalAmount = paymentRepository.getTotalAmountByPatient(patientId);
                            result.put("totalAmount", totalAmount != null ? totalAmount : BigDecimal.ZERO);

                            // Tính trung bình mỗi lần thanh toán
                            if (paymentCount > 0 && totalAmount != null) {
                                result.put("averageAmount", totalAmount.divide(BigDecimal.valueOf(paymentCount), 2, BigDecimal.ROUND_HALF_UP));
                            } else {
                                result.put("averageAmount", BigDecimal.ZERO);
                            }
                        }
                        return result;
                    })
                    .filter(map -> !map.isEmpty())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting top paying patients: ", e);
            return Collections.emptyList();
        }
    }

    /**
     * Top bác sĩ có doanh thu cao nhất
     */
    public List<Map<String, Object>> getTopRevenueGeneratingDoctors(int limit) {
        try {
            List<Object[]> rawData = paymentRepository.findTopRevenueGeneratingDoctors();

            return rawData.stream()
                    .limit(limit)
                    .map(row -> {
                        Long doctorId = (Long) row[0];
                        BigDecimal totalRevenue = (BigDecimal) row[1];

                        // Lấy thông tin bác sĩ
                        Optional<Doctor> doctorOpt = doctorRepository.findById(doctorId);

                        Map<String, Object> result = new HashMap<>();
                        if (doctorOpt.isPresent()) {
                            Doctor doctor = doctorOpt.get();
                            result.put("doctorId", doctorId);
                            result.put("doctorName", doctor.getFullName());
                            result.put("specialty", doctor.getSpecialty());
                            result.put("totalRevenue", totalRevenue);

                            // Đếm số lượng appointment đã thanh toán
                            long appointmentCount = paymentRepository.countCompletedPaymentsByDoctor(doctorId);
                            result.put("appointmentCount", appointmentCount);

                            // Tính trung bình doanh thu mỗi appointment
                            if (appointmentCount > 0) {
                                result.put("averageRevenue", totalRevenue.divide(BigDecimal.valueOf(appointmentCount), 2, BigDecimal.ROUND_HALF_UP));
                            } else {
                                result.put("averageRevenue", BigDecimal.ZERO);
                            }
                        }
                        return result;
                    })
                    .filter(map -> !map.isEmpty())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting top revenue generating doctors: ", e);
            return Collections.emptyList();
        }
    }

// Helper methods

    private byte[] generateExcelReport(List<Payment> payments, LocalDateTime startDate, LocalDateTime endDate) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Payment Report");

        // Create header style
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        // Create header row
        Row headerRow = sheet.createRow(0);
        String[] headers = {"ID", "Patient", "Doctor", "Amount", "Method", "Status", "Date", "Transaction ID"};

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Add data rows
        int rowNum = 1;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (Payment payment : payments) {
            Row row = sheet.createRow(rowNum++);

            row.createCell(0).setCellValue(payment.getId());
            row.createCell(1).setCellValue(payment.getPatient().getFullName());
            row.createCell(2).setCellValue(payment.getDoctor().getFullName());
            row.createCell(3).setCellValue(payment.getAmount().doubleValue());
            row.createCell(4).setCellValue(payment.getPaymentMethod().toString());
            row.createCell(5).setCellValue(payment.getStatus().toString());
            row.createCell(6).setCellValue(payment.getCreatedAt().format(formatter));
            row.createCell(7).setCellValue(payment.getTransactionId() != null ? payment.getTransactionId() : "");
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // Convert to byte array
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return outputStream.toByteArray();
    }

    private byte[] generatePdfReport(List<Payment> payments, LocalDateTime startDate, LocalDateTime endDate) throws DocumentException, IOException {
        Document document = new Document(PageSize.A4.rotate()); // Landscape for better table fit
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, outputStream);

        document.open();

        // Add title
        com.itextpdf.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.BLACK);
        Paragraph title = new Paragraph("Payment Report", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        // Add date range
        com.itextpdf.text.Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12, BaseColor.BLACK);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        Paragraph dateRange = new Paragraph(
                String.format("Period: %s to %s", startDate.format(formatter), endDate.format(formatter)),
                normalFont
        );
        dateRange.setAlignment(Element.ALIGN_CENTER);
        dateRange.setSpacingAfter(20);
        document.add(dateRange);

        // Create table
        PdfPTable table = new PdfPTable(8);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        table.setSpacingAfter(10f);

        // Set column widths
        float[] columnWidths = {1f, 2f, 2f, 1.5f, 1.5f, 1.5f, 2f, 2f};
        table.setWidths(columnWidths);

        // Add headers
        com.itextpdf.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.WHITE);
        String[] headers = {"ID", "Patient", "Doctor", "Amount", "Method", "Status", "Date", "Transaction ID"};

        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(BaseColor.DARK_GRAY);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(8);
            table.addCell(cell);
        }

        // Add data
        com.itextpdf.text.Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 9, BaseColor.BLACK);
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        for (Payment payment : payments) {
            table.addCell(new PdfPCell(new Phrase(payment.getId().toString(), dataFont)));
            table.addCell(new PdfPCell(new Phrase(payment.getPatient().getFullName(), dataFont)));
            table.addCell(new PdfPCell(new Phrase(payment.getDoctor().getFullName(), dataFont)));

            PdfPCell amountCell = new PdfPCell(new Phrase(
                    String.format("%,.0f VND", payment.getAmount().doubleValue()), dataFont));
            amountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(amountCell);

            table.addCell(new PdfPCell(new Phrase(payment.getPaymentMethod().toString(), dataFont)));

            PdfPCell statusCell = new PdfPCell(new Phrase(payment.getStatus().toString(), dataFont));
            if (payment.getStatus() == Payment.PaymentStatus.COMPLETED) {
                statusCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            }
            table.addCell(statusCell);

            table.addCell(new PdfPCell(new Phrase(payment.getCreatedAt().format(dateFormatter), dataFont)));
            table.addCell(new PdfPCell(new Phrase(
                    payment.getTransactionId() != null ? payment.getTransactionId() : "", dataFont)));
        }

        document.add(table);

        // Add summary
        BigDecimal totalAmount = payments.stream()
                .filter(p -> p.getStatus() == Payment.PaymentStatus.COMPLETED)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long completedCount = payments.stream()
                .filter(p -> p.getStatus() == Payment.PaymentStatus.COMPLETED)
                .count();

        Paragraph summary = new Paragraph(
                String.format("\nSummary:\nTotal Completed Payments: %d\nTotal Revenue: %,.0f VND",
                        completedCount, totalAmount.doubleValue()),
                normalFont
        );
        summary.setSpacingBefore(20);
        document.add(summary);

        document.close();
        return outputStream.toByteArray();
    }

    private String getMonthName(int month) {
        String[] monthNames = {
                "Tháng 1", "Tháng 2", "Tháng 3", "Tháng 4", "Tháng 5", "Tháng 6",
                "Tháng 7", "Tháng 8", "Tháng 9", "Tháng 10", "Tháng 11", "Tháng 12"
        };
        return monthNames[month - 1];
    }

    /**
     * Thống kê chi tiết theo ngày
     */
    public Map<String, Object> getDailyStatistics(LocalDate date) {
        try {
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.atTime(23, 59, 59);

            Map<String, Object> stats = new HashMap<>();

            // Tổng quan ngày
            List<Payment> dayPayments = paymentRepository.findPaymentsByDateRange(startOfDay, endOfDay);

            long totalPayments = dayPayments.size();
            long completedPayments = dayPayments.stream()
                    .filter(p -> p.getStatus() == Payment.PaymentStatus.COMPLETED)
                    .count();
            long failedPayments = dayPayments.stream()
                    .filter(p -> p.getStatus() == Payment.PaymentStatus.FAILED)
                    .count();
            long pendingPayments = dayPayments.stream()
                    .filter(p -> p.getStatus() == Payment.PaymentStatus.PENDING)
                    .count();

            BigDecimal totalRevenue = dayPayments.stream()
                    .filter(p -> p.getStatus() == Payment.PaymentStatus.COMPLETED)
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            stats.put("date", date);
            stats.put("totalPayments", totalPayments);
            stats.put("completedPayments", completedPayments);
            stats.put("failedPayments", failedPayments);
            stats.put("pendingPayments", pendingPayments);
            stats.put("totalRevenue", totalRevenue);
            stats.put("successRate", totalPayments > 0 ? (double) completedPayments / totalPayments * 100 : 0);

            // Thống kê theo giờ
            List<Object[]> hourlyStats = paymentRepository.getHourlyStatistics(startOfDay);
            Map<Integer, Long> hourlyData = new HashMap<>();
            for (Object[] stat : hourlyStats) {
                Integer hour = (Integer) stat[0];
                Long count = (Long) stat[1];
                hourlyData.put(hour, count);
            }
            stats.put("hourlyStatistics", hourlyData);

            // Thống kê theo phương thức thanh toán
            Map<Payment.PaymentMethod, Map<String, Object>> methodStats = new HashMap<>();
            for (Payment.PaymentMethod method : Payment.PaymentMethod.values()) {
                long methodCount = dayPayments.stream()
                        .filter(p -> p.getPaymentMethod() == method && p.getStatus() == Payment.PaymentStatus.COMPLETED)
                        .count();
                BigDecimal methodRevenue = dayPayments.stream()
                        .filter(p -> p.getPaymentMethod() == method && p.getStatus() == Payment.PaymentStatus.COMPLETED)
                        .map(Payment::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                Map<String, Object> methodData = new HashMap<>();
                methodData.put("count", methodCount);
                methodData.put("revenue", methodRevenue);
                methodData.put("percentage", completedPayments > 0 ? (double) methodCount / completedPayments * 100 : 0);

                methodStats.put(method, methodData);
            }
            stats.put("paymentMethodStats", methodStats);

            return stats;

        } catch (Exception e) {
            log.error("Error getting daily statistics: ", e);
            return new HashMap<>();
        }
    }

    /**
     * Thống kê theo phương thức thanh toán
     */
    public Map<String, Object> getPaymentMethodStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        try {
            List<Object[]> methodStats = paymentRepository.getPaymentMethodStatistics(startDate, endDate);

            Map<String, Object> result = new HashMap<>();
            Map<String, Map<String, Object>> methodData = new HashMap<>();

            BigDecimal totalRevenue = BigDecimal.ZERO;
            long totalCount = 0;

            for (Object[] stat : methodStats) {
                Payment.PaymentMethod method = (Payment.PaymentMethod) stat[0];
                Long count = (Long) stat[1];
                BigDecimal revenue = (BigDecimal) stat[2];

                Map<String, Object> data = new HashMap<>();
                data.put("count", count);
                data.put("revenue", revenue);
                data.put("averageAmount", count > 0 ? revenue.divide(BigDecimal.valueOf(count), 2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO);

                methodData.put(method.toString(), data);
                totalRevenue = totalRevenue.add(revenue);
                totalCount += count;
            }

            // Tính phần trăm cho mỗi phương thức
            for (Map.Entry<String, Map<String, Object>> entry : methodData.entrySet()) {
                Map<String, Object> data = entry.getValue();
                BigDecimal revenue = (BigDecimal) data.get("revenue");
                Long count = (Long) data.get("count");

                data.put("revenuePercentage", totalRevenue.compareTo(BigDecimal.ZERO) > 0 ?
                        revenue.divide(totalRevenue, 4, BigDecimal.ROUND_HALF_UP).multiply(BigDecimal.valueOf(100)) : BigDecimal.ZERO);
                data.put("countPercentage", totalCount > 0 ? (double) count / totalCount * 100 : 0);
            }

            result.put("methodStatistics", methodData);
            result.put("totalRevenue", totalRevenue);
            result.put("totalCount", totalCount);
            result.put("startDate", startDate);
            result.put("endDate", endDate);

            return result;

        } catch (Exception e) {
            log.error("Error getting payment method statistics: ", e);
            return new HashMap<>();
        }
    }

    /**
     * Dashboard overview
     */
    public Map<String, Object> getDashboardOverview() {
        try {
            Map<String, Object> overview = new HashMap<>();

            // Thống kê tổng quan
            PaymentStatisticsResponse stats = getPaymentStatistics();
            overview.put("totalStats", stats);

            // Thống kê hôm nay
            LocalDate today = LocalDate.now();
            Map<String, Object> todayStats = getDailyStatistics(today);
            overview.put("todayStats", todayStats);

            // Thống kê tuần này
            LocalDateTime startOfWeek = today.atStartOfDay().minusDays(today.getDayOfWeek().getValue() - 1);
            LocalDateTime endOfWeek = startOfWeek.plusDays(6).withHour(23).withMinute(59).withSecond(59);

            List<Payment> weekPayments = paymentRepository.findPaymentsByDateRange(startOfWeek, endOfWeek);
            BigDecimal weekRevenue = weekPayments.stream()
                    .filter(p -> p.getStatus() == Payment.PaymentStatus.COMPLETED)
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Map<String, Object> weekStats = new HashMap<>();
            weekStats.put("revenue", weekRevenue);
            weekStats.put("count", weekPayments.stream().filter(p -> p.getStatus() == Payment.PaymentStatus.COMPLETED).count());
            overview.put("weekStats", weekStats);

            // Thống kê tháng này
            LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();
            LocalDateTime endOfMonth = today.withDayOfMonth(today.lengthOfMonth()).atTime(23, 59, 59);

            List<Payment> monthPayments = paymentRepository.findPaymentsByDateRange(startOfMonth, endOfMonth);
            BigDecimal monthRevenue = monthPayments.stream()
                    .filter(p -> p.getStatus() == Payment.PaymentStatus.COMPLETED)
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Map<String, Object> monthStats = new HashMap<>();
            monthStats.put("revenue", monthRevenue);
            monthStats.put("count", monthPayments.stream().filter(p -> p.getStatus() == Payment.PaymentStatus.COMPLETED).count());
            overview.put("monthStats", monthStats);

            // Recent failed payments (24h)
            LocalDateTime yesterday = LocalDateTime.now().minusHours(24);
            List<PaymentResponse> recentFailed = getFailedPayments(yesterday, LocalDateTime.now());
            overview.put("recentFailedPayments", recentFailed.size());

            // Pending payments count
            long pendingCount = paymentRepository.countByStatus(Payment.PaymentStatus.PENDING);
            overview.put("pendingPayments", pendingCount);

            // Top payment methods (last 30 days)
            LocalDateTime last30Days = LocalDateTime.now().minusDays(30);
            Map<String, Object> methodStats = getPaymentMethodStatistics(last30Days, LocalDateTime.now());
            overview.put("topPaymentMethods", methodStats);

            // Growth metrics (compare with last month)
            LocalDateTime lastMonthStart = startOfMonth.minusMonths(1);
            LocalDateTime lastMonthEnd = startOfMonth.minusSeconds(1);

            List<Payment> lastMonthPayments = paymentRepository.findPaymentsByDateRange(lastMonthStart, lastMonthEnd);
            BigDecimal lastMonthRevenue = lastMonthPayments.stream()
                    .filter(p -> p.getStatus() == Payment.PaymentStatus.COMPLETED)
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            long lastMonthCount = lastMonthPayments.stream()
                    .filter(p -> p.getStatus() == Payment.PaymentStatus.COMPLETED)
                    .count();

            // Calculate growth
            double revenueGrowth = 0;
            double countGrowth = 0;

            if (lastMonthRevenue.compareTo(BigDecimal.ZERO) > 0) {
                revenueGrowth = monthRevenue.subtract(lastMonthRevenue)
                        .divide(lastMonthRevenue, 4, BigDecimal.ROUND_HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue();
            }

            if (lastMonthCount > 0) {
                long currentMonthCount = monthPayments.stream()
                        .filter(p -> p.getStatus() == Payment.PaymentStatus.COMPLETED)
                        .count();
                countGrowth = ((double) (currentMonthCount - lastMonthCount) / lastMonthCount) * 100;
            }

            Map<String, Object> growth = new HashMap<>();
            growth.put("revenueGrowth", revenueGrowth);
            growth.put("countGrowth", countGrowth);
            overview.put("growth", growth);

            return overview;

        } catch (Exception e) {
            log.error("Error getting dashboard overview: ", e);
            return new HashMap<>();
        }
    }

    /**
     * Cleanup old pending payments
     */
    @Transactional
    public void cleanupStalePendingPayments() {
        try {
            List<Payment> stalePayments = getStalePendingPayments().stream()
                    .map(response -> paymentRepository.findById(response.getPaymentId()))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());

            for (Payment payment : stalePayments) {
                payment.setStatus(Payment.PaymentStatus.FAILED);
                paymentRepository.save(payment);
                log.info("Marked stale pending payment as failed: {}", payment.getId());
            }

        } catch (Exception e) {
            log.error("Error cleaning up stale pending payments: ", e);
        }
    }
}