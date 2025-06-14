package com.hospital.telemedicine.entity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Entity
@Table(name = "patients")
@NoArgsConstructor
@AllArgsConstructor
public class Patient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @Column(nullable = false)
    private String phone;

    private String address;

    public enum Gender {
        MALE, FEMALE, OTHER
    }
}
