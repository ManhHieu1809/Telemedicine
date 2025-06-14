package com.hospital.telemedicine.repository;

import com.hospital.telemedicine.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.roles = :role")
    List<User> findByRole(@Param("role") User.UserRole role);

    @Query("SELECT u FROM User u WHERE u.username LIKE %:keyword% OR u.email LIKE %:keyword%")
    List<User> searchUsers(@Param("keyword") String keyword);

    @Query("SELECT u FROM User u WHERE u.id IN :userIds")
    List<User> findUsersByIds(@Param("userIds") List<Long> userIds);
}