package com.hospital.telemedicine.controller;

import com.hospital.telemedicine.dto.response.FavoriteDoctorResponse;
import com.hospital.telemedicine.security.UserDetailsImpl;
import com.hospital.telemedicine.service.FavoriteDoctorService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteDoctorController {

    private final FavoriteDoctorService favoriteDoctorService;

    public FavoriteDoctorController(FavoriteDoctorService favoriteDoctorService) {
        this.favoriteDoctorService = favoriteDoctorService;
    }

    @PostMapping("/{doctorUserId}")
    public ResponseEntity<FavoriteDoctorResponse> addFavoriteDoctor(
            @PathVariable Long doctorUserId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = extractUserId(userDetails);
        System.out.println("Controller: Adding favorite doctor for userId: " + userId + ", doctorUserId: " + doctorUserId);
        FavoriteDoctorResponse response = favoriteDoctorService.addFavoriteDoctor(userId, doctorUserId);
        System.out.println("Controller: Response: " + response);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{doctorUserId}")
    public ResponseEntity<FavoriteDoctorResponse> removeFavoriteDoctor(
            @PathVariable Long doctorUserId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = extractUserId(userDetails);
        System.out.println("Controller: Removing favorite doctor for userId: " + userId + ", doctorUserId: " + doctorUserId);
        FavoriteDoctorResponse response = favoriteDoctorService.removeFavoriteDoctor(userId, doctorUserId);
        System.out.println("Controller: Response: " + response);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<FavoriteDoctorResponse>> getFavoriteDoctors(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = extractUserId(userDetails);
        System.out.println("Controller: Getting favorite doctors for userId: " + userId);
        List<FavoriteDoctorResponse> favorites = favoriteDoctorService.getFavoriteDoctors(userId);
        System.out.println("Controller: Favorites: " + favorites);
        return ResponseEntity.ok(favorites);
    }

    private Long extractUserId(UserDetails userDetails) {
        if (userDetails instanceof UserDetailsImpl) {
            return ((UserDetailsImpl) userDetails).getId();
        }
        throw new IllegalArgumentException("UserDetails must be an instance of UserDetailsImpl");
    }
}