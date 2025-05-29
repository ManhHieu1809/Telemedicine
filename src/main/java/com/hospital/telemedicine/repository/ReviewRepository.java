package com.hospital.telemedicine.repository;


import com.hospital.telemedicine.entity.Reviews;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Reviews, Long> {
    List<Reviews> findByPatientId(Long patientId);
    List<Reviews> findByDoctorId(Long doctorId);
}