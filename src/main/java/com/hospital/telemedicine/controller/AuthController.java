package com.hospital.telemedicine.controller;

import com.hospital.telemedicine.dto.request.*;
import com.hospital.telemedicine.dto.response.AuthResponse;
import com.hospital.telemedicine.dto.response.MessageResponse;
import com.hospital.telemedicine.service.AuthService;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        AuthResponse response = authService.authenticateUser(loginRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        MessageResponse response = authService.registerUser(registerRequest);
        return ResponseEntity.ok(response);
    }

    // Endpoint để admin tạo tài khoản bác sĩ
    @PostMapping("/create-doctor")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createDoctorAccount(
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String fullName,
            @RequestParam String specialty,
            @RequestParam Integer experience,
            @RequestParam String phone,
            @RequestParam(required = false) String address) {

        MessageResponse response = authService.createDoctorAccount(
                username, email, password, fullName, specialty, experience, phone, address);

        return ResponseEntity.ok(response);
    }





}