// src/main/java/com/hospital/telemedicine/config/PaymentConfig.java
package com.hospital.telemedicine.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "payment")
public class PaymentConfig {

    private VnpayConfig vnpay = new VnpayConfig();
    private MomoConfig momo = new MomoConfig();

    @Data
    public static class VnpayConfig {
        private String payUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
        private String returnUrl = "http://localhost:8080/api/payments/vnpay/callback";
        private String tmnCode;
        private String secretKey;
        private String version = "2.1.0";
        private String command = "pay";
        private String orderType = "other";
    }

    @Data
    public static class MomoConfig {
        private String paymentUrl = "https://test-payment.momo.vn/v2/gateway/api/create";
        private String notifyUrl = "http://localhost:8080/api/payments/momo/callback";
        private String returnUrl = "http://localhost:8080/api/payments/momo/return";
        private String partnerCode;
        private String accessKey;
        private String secretKey;
        private String requestType = "payWithMethod";
    }
}