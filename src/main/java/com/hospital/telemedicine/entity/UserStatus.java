 package com.hospital.telemedicine.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "user_status")
public class UserStatus {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "is_online")
    private boolean isOnline;

    @Column(name = "last_seen")
    private LocalDateTime lastSeen;

    @MapsId
    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;
}
