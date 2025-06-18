package com.hospital.telemedicine.dto.response;

import com.hospital.telemedicine.entity.Payment;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponse {
    private boolean success;
    private String message;
    private Long paymentId;
    private Long appointmentId;
    private BigDecimal amount;
    private Payment.PaymentMethod paymentMethod;
    private Payment.PaymentStatus status;
    private String paymentUrl;
    private String transactionId;
    private String patientName;
    private String doctorName;
    private LocalDateTime createdAt;
}