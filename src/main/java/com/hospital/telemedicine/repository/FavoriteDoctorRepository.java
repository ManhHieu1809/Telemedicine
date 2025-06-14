package com.hospital.telemedicine.repository;

import com.hospital.telemedicine.entity.FavoriteDoctor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteDoctorRepository extends JpaRepository<FavoriteDoctor, Long> {
    Optional<FavoriteDoctor> findByPatientIdAndDoctorId(Long patientId, Long doctorId);
    List<FavoriteDoctor> findByPatientId(Long patientId);
}
