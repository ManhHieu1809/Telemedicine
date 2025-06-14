package com.hospital.telemedicine.controller;

import com.hospital.telemedicine.dto.request.*;
import com.hospital.telemedicine.dto.response.AuthResponse;
import com.hospital.telemedicine.dto.response.MessageResponse;
import com.hospital.telemedicine.service.AuthService;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {

        boolean emailEmpty = (loginRequest.getEmail() == null || loginRequest.getEmail().trim().isEmpty());
        boolean passwordEmpty = (loginRequest.getPassword() == null || loginRequest.getPassword().trim().isEmpty());

        if (emailEmpty || passwordEmpty) {
            String message;
            if (emailEmpty && passwordEmpty) {
                message = "Email và Password không được để trống";
            } else if (emailEmpty) {
                message = "Email không được để trống";
            } else {
                message = "Password không được để trống";
            }

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", message);
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);  // 400
        }

        try {
            AuthResponse authResponse = authService.authenticateUser(loginRequest);
            return new ResponseEntity<>(authResponse, HttpStatus.OK);
        } catch (AuthenticationException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Email hoặc mật khẩu không hợp lệ");
            return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);  // 401
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
        }
    }



    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        MessageResponse response = authService.registerUser(registerRequest);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
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

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody ChangePasswordRequest request) {
        Long userId = extractUserId(userDetails);
        authService.changePassword(userId, request);
        return ResponseEntity.ok().build();
    }

    private Long extractUserId(UserDetails userDetails) {
        if (userDetails instanceof com.hospital.telemedicine.security.UserDetailsImpl) {
            return ((com.hospital.telemedicine.security.UserDetailsImpl) userDetails).getId();
        }
        throw new IllegalArgumentException("UserDetails must be an instance of UserDetailsImpl");
    }

}