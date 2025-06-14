package com.hospital.telemedicine.dto.request;

import lombok.Data;

import java.time.LocalTime;
@Data
public class DoctorScheduleRequest {
    private Long doctorId;
    private String dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
}
