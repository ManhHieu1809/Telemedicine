package com.hospital.telemedicine.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "conversation_participants")
public class ConversationParticipant {
    @Id
    @ManyToOne
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Id
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
