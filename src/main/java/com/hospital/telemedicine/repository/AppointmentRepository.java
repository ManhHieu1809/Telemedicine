package com.hospital.telemedicine.repository;

import com.hospital.telemedicine.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByDoctorUserId(Long userId);

    List<Appointment> findByPatientId(Long patientId);

    @Query("SELECT a FROM Appointment a WHERE a.doctor.id = :doctorId AND a.date = :date " +
            "AND a.time BETWEEN :startTime AND :endTime")
    List<Appointment> findByDoctorIdAndDateTimeBetween(Long doctorId, LocalDate date, LocalTime startTime, LocalTime endTime);

    // Lấy danh sách cuộc hẹn của bác sĩ trong một ngày (từ yêu cầu trước)
    @Query("SELECT a FROM Appointment a WHERE a.doctor.id = :doctorId AND a.date = :date")
    List<Appointment> findByDoctorIdAndDate(Long doctorId, LocalDate date);

    // Kiểm tra xem bác sĩ có cuộc hẹn tại thời điểm cụ thể không (từ yêu cầu trước)
    @Query("SELECT COUNT(a) > 0 FROM Appointment a WHERE a.doctor.id = :doctorId AND a.date = :date AND a.time = :time")
    boolean existsByDoctorIdAndDateAndTime(Long doctorId, LocalDate date, LocalTime time);
}