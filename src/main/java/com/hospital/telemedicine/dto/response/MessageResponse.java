package com.hospital.telemedicine.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MessageResponse {
    private String message;
    private boolean success;
}
