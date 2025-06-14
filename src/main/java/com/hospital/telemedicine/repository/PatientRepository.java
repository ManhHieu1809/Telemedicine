package com.hospital.telemedicine.repository;

import com.hospital.telemedicine.entity.Patient;
import com.hospital.telemedicine.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByUserId(Long userId);

}
