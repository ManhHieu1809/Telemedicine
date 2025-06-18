package com.hospital.telemedicine.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

@Slf4j
@Component
public class PaymentSecurityUtils {

    /**
     * Generate secure random string for transaction ID
     */
    public String generateTransactionId() {
        return "TXN" + System.currentTimeMillis() + "_" +
                (int)(Math.random() * 10000);
    }

    /**
     * Validate IP address từ trusted payment gateways
     */
    public boolean isValidPaymentGatewayIP(String ipAddress) {
        // VNPay IP ranges (cần cập nhật từ VNPay documentation)
        List<String> vnpayIPs = Arrays.asList(
                "203.171.21.0/24",
                "203.171.22.0/24"
        );

        // MoMo IP ranges (cần cập nhật từ MoMo documentation)
        List<String> momoIPs = Arrays.asList(
                "113.160.92.0/24"
        );

        // Implement IP range checking logic
        return isIPInRanges(ipAddress, vnpayIPs) || isIPInRanges(ipAddress, momoIPs);
    }

    /**
     * Encrypt sensitive payment data
     */
    public String encryptPaymentData(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            log.error("Error encrypting payment data: ", e);
            return null;
        }
    }

    /**
     * Generate checksum cho payment request
     */
    public String generateChecksum(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            log.error("Error generating checksum: ", e);
            return null;
        }
    }

    /**
     * Validate amount to prevent tampering
     */
    public boolean validateAmount(String originalAmount, String receivedAmount, String signature, String secretKey) {
        try {
            String expectedSignature = encryptPaymentData(originalAmount, secretKey);
            return expectedSignature != null && expectedSignature.equals(signature);
        } catch (Exception e) {
            log.error("Error validating amount: ", e);
            return false;
        }
    }

    private boolean isIPInRanges(String ipAddress, List<String> ipRanges) {
        // Simplified IP range checking - implement proper CIDR matching
        for (String range : ipRanges) {
            if (range.contains("/")) {
                String baseIP = range.split("/")[0];
                if (ipAddress.startsWith(baseIP.substring(0, baseIP.lastIndexOf(".")))) {
                    return true;
                }
            } else if (ipAddress.equals(range)) {
                return true;
            }
        }
        return false;
    }
}