package com.hospital.telemedicine.dto.request;

import com.hospital.telemedicine.entity.Payment;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentRequest {
    private Long appointmentId;
    private BigDecimal amount;
    private Payment.PaymentMethod paymentMethod;
    private String ipAddress;
    private String description;
}