package com.hospital.telemedicine.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardStatsResponse {
    private long totalUsers;
    private long totalDoctors;
    private long totalPatients;
    private long totalAppointments;
}