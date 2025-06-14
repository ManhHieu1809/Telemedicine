package com.hospital.telemedicine.repository;

import com.hospital.telemedicine.entity.UserFcmToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserFcmTokenRepository extends JpaRepository<UserFcmToken, Long> {
    List<UserFcmToken> findByUserId(Long userId);
    void deleteByFcmToken(String fcmToken);
}