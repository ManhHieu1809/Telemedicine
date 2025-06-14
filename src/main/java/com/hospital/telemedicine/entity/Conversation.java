package com.hospital.telemedicine.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "conversations")
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToMany
    @JoinTable(
            name = "conversation_participants",
            joinColumns = @JoinColumn(name = "conversation_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> participants;

    @Column(nullable = false)
    private String lastMessage;

    @Column(nullable = false)
    private LocalDateTime lastActive;

    @Column(nullable = true)
    private Boolean isGroup;

    @Column(nullable = true)
    private String name;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}