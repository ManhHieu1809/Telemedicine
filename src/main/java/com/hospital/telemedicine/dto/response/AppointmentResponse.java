package com.hospital.telemedicine.dto.response;

import com.hospital.telemedicine.entity.Appointment;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class AppointmentResponse {
    private Long id;
    private Long patientId;
    private String patientName;
    private Long doctorId;
    private String doctorName;
    private LocalDate date;
    private LocalTime time;
    private Appointment.Status status;
    private Appointment.AppointmentType appointmentType;
    private LocalDateTime createdAt;
    private boolean success;
    private String message;

    // Constructor cho trường hợp thành công
    public AppointmentResponse(Long id, Long patientId, String patientName, Long doctorId, String doctorName,
                               LocalDate date,LocalTime time, Appointment.Status status,Appointment.AppointmentType appointmentType,
                               LocalDateTime createdAt, String message) {
        this.id = id;
        this.patientId = patientId;
        this.patientName = patientName;
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.date = date;
        this.time = time;
        this.status = status;
        this.appointmentType = appointmentType;
        this.createdAt = createdAt;
        this.success = true;
        this.message = message;
    }
    // Constructor cho trường hợp thất bại
    public AppointmentResponse(String errorMessage) {
        this.success = false;
        this.message = errorMessage;
    }

    // Constructor mặc định (cho trường hợp thất bại không có thông tin)
    public AppointmentResponse() {
        this.success = false;
        this.message = "Operation failed";
    }
}
