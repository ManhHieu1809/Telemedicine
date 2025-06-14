package com.hospital.telemedicine.repository;
import com.hospital.telemedicine.entity.UserActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserActivityRepository extends JpaRepository<UserActivity, Long> {
    List<UserActivity> findByUserId(Long userId);
}