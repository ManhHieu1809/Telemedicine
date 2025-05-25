package com.hospital.telemedicine.dto.request;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatMessageRequest {
    private Long senderId;
    private Long receiverId;
    private String content;
    private LocalDateTime timestamp;
}
