package com.hospital.telemedicine.repository;

import com.hospital.telemedicine.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserStatusRepository extends JpaRepository<UserStatus, Long> {

    @Query("SELECT us FROM UserStatus us WHERE us.userId = :userId")
    Optional<UserStatus> findByUserId(@Param("userId") Long userId);

    @Query("SELECT us FROM UserStatus us WHERE us.isOnline = true")
    List<UserStatus> findAllOnlineUsers();

    @Query("SELECT us FROM UserStatus us WHERE us.userId IN :userIds")
    List<UserStatus> findUserStatusByUserIds(@Param("userIds") List<Long> userIds);

    @Modifying
    @Transactional
    @Query("DELETE FROM UserStatus us WHERE us.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    boolean existsByUserId(Long userId);
}