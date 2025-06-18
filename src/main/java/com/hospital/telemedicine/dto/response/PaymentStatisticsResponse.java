
package com.hospital.telemedicine.dto.response;

import com.hospital.telemedicine.entity.Payment;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
public class PaymentStatisticsResponse {
    private long totalPayments;
    private long completedPayments;
    private long failedPayments;
    private long pendingPayments;
    private BigDecimal totalRevenue;
    private Map<Payment.PaymentMethod, BigDecimal> revenueByMethod;
    private double successRate;
    private BigDecimal averagePaymentAmount;
    private Map<String, Object> monthlyStatistics;
    private Map<String, Object> dailyStatistics;
}