// File: src/main/java/com/hospital/telemedicine/entity/UserFcmToken.java
package com.hospital.telemedicine.entity;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "user_fcm_tokens")
public class UserFcmToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "fcm_token", nullable = false)
    private String fcmToken;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
