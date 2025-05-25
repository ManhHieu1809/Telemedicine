package com.hospital.telemedicine.dto.request;

import com.hospital.telemedicine.entity.Appointment;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class AppointmentRequest {
    private Long doctorId;
    private Long patientId;
    private LocalDate date;
    private LocalTime time;
    private Appointment.AppointmentType appointmentType;
}
