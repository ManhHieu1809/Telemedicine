package com.hospital.telemedicine.repository;

import com.hospital.telemedicine.entity.DoctorSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalTime;
import java.util.List;

public interface DoctorScheduleRepository extends JpaRepository<DoctorSchedule, Long> {
    List<DoctorSchedule> findByDoctorIdAndDayOfWeek(Long doctorId, DoctorSchedule.DayOfWeek dayOfWeek);
}
