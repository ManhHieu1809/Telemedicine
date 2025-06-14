package com.hospital.telemedicine.controller;

import com.hospital.telemedicine.dto.request.ReviewRequest;
import com.hospital.telemedicine.dto.response.ApiResponse;
import com.hospital.telemedicine.dto.response.ReviewResponse;
import com.hospital.telemedicine.security.UserDetailsImpl;
import com.hospital.telemedicine.service.ReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<Map<String, Object>> getReviewsByDoctorId(@PathVariable Long doctorId) {
        List<ReviewResponse> reviews = reviewService.getReviewsByDoctorId(doctorId);
        Map<String, Object> response = new HashMap<>();
        response.put("suscess", true);
        response.put("data", reviews);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReviewResponse>> addReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody ReviewRequest request) {
        Long patientId = extractPatientId(userDetails);
        ReviewResponse review = reviewService.addReview(patientId, request);
        return ResponseEntity.ok(new ApiResponse<>(true, review));
    }

    private Long extractPatientId(UserDetails userDetails) {
        if (userDetails instanceof UserDetailsImpl userDetailsImpl) {
            return userDetailsImpl.getPatientId();
        }
        throw new IllegalArgumentException("User details do not contain patient ID");
    }
}
