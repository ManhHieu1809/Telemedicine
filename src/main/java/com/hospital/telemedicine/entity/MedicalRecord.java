package com.hospital.telemedicine.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "medical_records")
public class MedicalRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Column(name = "diagnosis", nullable = false)
    private String diagnosis;

    @ManyToOne
    @JoinColumn(name = "prescription_id")
    private Prescription prescription;

    @Column(name = "record_date")
    private LocalDateTime recordDate;
}