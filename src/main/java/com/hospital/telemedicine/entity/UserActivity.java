package com.hospital.telemedicine.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "user_activities")
public class UserActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "activity_type", nullable = false)
    private String activityType; // LOGIN, APPOINTMENT, etc.

    @Column(nullable = false)
    private String description;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
}