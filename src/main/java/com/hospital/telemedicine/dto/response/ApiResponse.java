package com.hospital.telemedicine.dto.response;

import lombok.Data;

@Data
public class ApiResponse<T> {
    private boolean success;
    private T data;
    public ApiResponse(boolean success, T data) {
        this.success = success;
        this.data = data;
    }

}
