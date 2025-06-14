package com.hospital.telemedicine.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "prescriptions")
public class Prescription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "notes")
    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL)
    private List<PrescriptionDetail> details;
    @Column(name = "prescribed_date")
    private LocalDateTime prescribedDate;
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}