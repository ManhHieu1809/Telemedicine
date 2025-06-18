
package com.hospital.telemedicine.util;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
public class VnpayUtil {

    public static String hmacSHA512(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder result = new StringBuilder();
            for (byte b : hash) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        } catch (Exception e) {
            log.error("Error generating HMAC SHA512: ", e);
            return "";
        }
    }

    public static boolean verifySignature(Map<String, String> params, String secretKey) {
        try {
            String vnpSecureHash = params.get("vnp_SecureHash");
            params.remove("vnp_SecureHash");
            params.remove("vnp_SecureHashType");

            // Sort parameters
            Map<String, String> sortedParams = new TreeMap<>(params);

            // Build hash data
            StringBuilder hashData = new StringBuilder();
            for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                    hashData.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
                }
            }

            if (hashData.length() > 0) {
                hashData.setLength(hashData.length() - 1); // Remove last &
            }

            String calculatedHash = hmacSHA512(secretKey, hashData.toString());
            return calculatedHash.equalsIgnoreCase(vnpSecureHash);

        } catch (Exception e) {
            log.error("Error verifying VNPay signature: ", e);
            return false;
        }
    }
}