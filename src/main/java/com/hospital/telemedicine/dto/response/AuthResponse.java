package com.hospital.telemedicine.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String type = "Bearer";
    private Long id;
    private String username;
    private String email;
    private String role;
    private String avatarUrl;
    private boolean success;
    public AuthResponse(String token, Long id, String username, String email, String role, String avatarUrl) {
        this.token = token;
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.avatarUrl = avatarUrl;
        this.success = true;
    }

    public AuthResponse() {
        this.success = false; // Thất bại
    }

}
