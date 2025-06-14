package com.hospital.telemedicine.repository;
import com.hospital.telemedicine.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}