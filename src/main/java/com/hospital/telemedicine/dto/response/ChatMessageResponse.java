package com.hospital.telemedicine.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatMessageResponse {
    private Long id;
    private Long senderId;
    private String senderName;
    private Long receiverId;
    private String receiverName;
    private String content;
    private LocalDateTime timestamp;
    private String status;
}
