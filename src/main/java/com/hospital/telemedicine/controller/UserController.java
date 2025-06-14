package com.hospital.telemedicine.controller;

import com.google.protobuf.Api;
import com.hospital.telemedicine.dto.request.UpdateUserRequest;
import com.hospital.telemedicine.dto.response.*;
import com.hospital.telemedicine.entity.User;
import com.hospital.telemedicine.repository.UserRepository;
import com.hospital.telemedicine.security.UserDetailsImpl;
import com.hospital.telemedicine.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;


    @PutMapping("/avatar")
    public ResponseEntity<String> updateAvatar(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) throws IOException {
        Long userId = extractUserId(userDetails);
        String avatarUrl = userService.updateAvatar(userId, file);
        return ResponseEntity.ok(avatarUrl);
    }

    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<ApiResponse<DoctorResponse>> getDoctorById(@PathVariable Long doctorId) {
        DoctorResponse doctor = userService.getDoctorById(doctorId);
        return ResponseEntity.ok(new ApiResponse<>(true, doctor));
    }

    @GetMapping("/doctors/specialty")
    public ResponseEntity<ApiResponse<List<DoctorResponse>>> getDoctorsBySpecialty(@RequestParam String specialty) {
        List<DoctorResponse> doctors = userService.getDoctorsBySpecialty(specialty);
        return ResponseEntity.ok(new ApiResponse<>(true, doctors));
    }

    @GetMapping("/doctors")
    public ResponseEntity<ApiResponse<List<DoctorResponse>>> getAllDoctors() {
        List<DoctorResponse> doctors = userService.getAllDoctors();
        return ResponseEntity.ok(new ApiResponse<>(true, doctors));
    }

    @PutMapping("/profile")
    public ResponseEntity<Map<String, Object>> updateUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateUserRequest request) {
        Long userId = extractUserId(userDetails);
        userService.updateUser(userId, request);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Cập nhật thành công");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/top-doctors")
    public ResponseEntity<ApiResponse<List<TopDoctorResponse>>> getTopDoctors() {
        List<TopDoctorResponse> doctors = userService.getTopDoctors();
        return ResponseEntity.ok(new ApiResponse<>(true, doctors));
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> getUserProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = extractUserId(userDetails);
        UserResponse user = userService.getUserProfile(userId);
        return ResponseEntity.ok(new ApiResponse<>(true, user));
    }

    @GetMapping("/doctor/{doctorId}/details")
    public ResponseEntity<ApiResponse<DoctorDetailsResponse>> getDoctorDetails(@PathVariable Long doctorId) {
        DoctorDetailsResponse doctorDetails = userService.getDoctorDetails(doctorId);
        return ResponseEntity.ok(new ApiResponse<>(true, doctorDetails));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long requesterId = extractUserId(userDetails);
        UserResponse userResponse = userService.getUserById(requesterId, userId);
        return ResponseEntity.ok(new ApiResponse<>(true, userResponse));
    }

    private Long extractUserId(UserDetails userDetails) {
        if (userDetails instanceof UserDetailsImpl) {
            return ((UserDetailsImpl) userDetails).getId();
        }
        throw new IllegalArgumentException("UserDetails must be an instance of UserDetailsImpl");
    }

}