package com.hospital.telemedicine.dto.response;

import lombok.Data;

@Data
public class AppointmentReport {
    private long totalAppointments;
    private long pendingAppointments;
    private long confirmedAppointments;
    private long cancelledAppointments;
    private long completedAppointments;
}
