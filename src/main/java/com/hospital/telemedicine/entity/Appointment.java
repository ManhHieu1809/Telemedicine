package com.hospital.telemedicine.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Entity
@Table(name = "appointments")
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "time", nullable = false)
    private LocalTime time;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Enumerated(EnumType.STRING)
    @Column(name = "appointment_type", nullable = false)
    private AppointmentType appointmentType;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public enum Status {
        PENDING, CONFIRMED, CANCELLED, COMPLETED
    }

    public enum AppointmentType {
        IN_PERSON, VIDEO_CALL
    }
}
