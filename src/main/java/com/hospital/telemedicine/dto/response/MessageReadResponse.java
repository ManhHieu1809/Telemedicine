package com.hospital.telemedicine.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MessageReadResponse {
    private Long messageId;
    private Long readBy;
    private String readByAvatar;
    private LocalDateTime readAt;
    private String status; // "READ"
}