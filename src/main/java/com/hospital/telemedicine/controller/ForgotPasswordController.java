package com.hospital.telemedicine.controller;

import com.hospital.telemedicine.dto.request.ForgotPasswordRequest;
import com.hospital.telemedicine.dto.request.ResetPasswordRequest;
import com.hospital.telemedicine.dto.request.VerifyOTPRequest;
import com.hospital.telemedicine.dto.response.MessageResponse;
import com.hospital.telemedicine.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/password")
public class ForgotPasswordController {

    @Autowired
    private AuthService authService;

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> requestPasswordReset(@RequestBody ForgotPasswordRequest request) {
        MessageResponse response = authService.requestPasswordReset(request.getEmail());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<MessageResponse> verifyOTP(@RequestBody VerifyOTPRequest request) {
        MessageResponse response = authService.verifyOTP(request.getOtp());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getOtp(), request.getNewPassword());
        return ResponseEntity.ok(new MessageResponse("Đặt lại mật khẩu thành công!", true));
    }
}