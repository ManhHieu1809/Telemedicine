package com.hospital.telemedicine.repository;

import com.hospital.telemedicine.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface UserStatusRepository extends JpaRepository<UserStatus, Long> {
    @Modifying
    @Query("UPDATE UserStatus us SET us.isOnline = true WHERE us.userId = :userId")
    void updateUserStatusToOnline(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE UserStatus us SET us.isOnline = false, us.lastSeen = :lastSeen WHERE us.userId = :userId")
    void updateUserStatusToOffline(@Param("userId") Long userId, @Param("lastSeen") LocalDateTime lastSeen);

    @Query("SELECT us FROM UserStatus us WHERE us.isOnline = true")
    List<UserStatus> findAllOnlineUsers();

    @Query("SELECT us FROM UserStatus us WHERE us.userId IN :userIds")
    List<UserStatus> findUserStatusByUserIds(@Param("userIds") List<Long> userIds);
}