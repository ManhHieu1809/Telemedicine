package com.hospital.telemedicine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class PaymentSecurityConfig {

    /**
     * CORS configuration cho payment endpoints
     */
    @Bean
    public CorsConfigurationSource paymentCorsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Cho phép origins từ payment gateways
        configuration.setAllowedOriginPatterns(Arrays.asList(
                "https://sandbox.vnpayment.vn",
                "https://vnpayment.vn",
                "https://test-payment.momo.vn",
                "https://payment.momo.vn",
                "http://localhost:3000", // Frontend dev
                "https://yourdomain.com" // Production frontend
        ));

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/payments/**", configuration);

        return source;
    }
}