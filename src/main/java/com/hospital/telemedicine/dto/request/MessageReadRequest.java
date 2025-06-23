package com.hospital.telemedicine.dto.request;

import lombok.Data;

@Data
public class MessageReadRequest {
    private Long messageId;
    private Long readerId;
}