package com.hospital.telemedicine.repository;
import com.hospital.telemedicine.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByPatientIdOrderByCreatedAtDesc(Long patientId);

    // Tìm thanh toán theo bác sĩ
    List<Payment> findByDoctorIdOrderByCreatedAtDesc(Long doctorId);

    // Tìm thanh toán theo appointment
    List<Payment> findByAppointmentId(Long appointmentId);

    // Đếm theo trạng thái
    long countByStatus(Payment.PaymentStatus status);

    // Tính tổng doanh thu
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'COMPLETED'")
    BigDecimal getTotalRevenue();

    // Doanh thu theo phương thức thanh toán
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'COMPLETED' AND p.paymentMethod = :method")
    BigDecimal getRevenueByPaymentMethod(@Param("method") Payment.PaymentMethod method);

    // Doanh thu theo khoảng thời gian
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'COMPLETED' AND p.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal getRevenueBetweenDates(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Thanh toán trong ngày
    @Query("SELECT p FROM Payment p WHERE DATE(p.createdAt) = DATE(:date)")
    List<Payment> findPaymentsByDate(@Param("date") LocalDateTime date);

    // Thanh toán trong tháng
    @Query("SELECT p FROM Payment p WHERE YEAR(p.createdAt) = :year AND MONTH(p.createdAt) = :month")
    List<Payment> findPaymentsByMonth(@Param("year") int year, @Param("month") int month);

    // Top bệnh nhân có nhiều thanh toán nhất
    @Query("SELECT p.patient.id, COUNT(p) as paymentCount FROM Payment p WHERE p.status = 'COMPLETED' GROUP BY p.patient.id ORDER BY paymentCount DESC")
    List<Object[]> findTopPayingPatients();

    // Top bác sĩ có nhiều doanh thu nhất
    @Query("SELECT p.doctor.id, SUM(p.amount) as totalRevenue FROM Payment p WHERE p.status = 'COMPLETED' GROUP BY p.doctor.id ORDER BY totalRevenue DESC")
    List<Object[]> findTopRevenueGeneratingDoctors();

    // Thanh toán thất bại trong khoảng thời gian
    @Query("SELECT p FROM Payment p WHERE p.status = 'FAILED' AND p.createdAt BETWEEN :startDate AND :endDate")
    List<Payment> findFailedPaymentsBetweenDates(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Thanh toán chờ xử lý quá lâu
    @Query("SELECT p FROM Payment p WHERE p.status = 'PENDING' AND p.createdAt < :cutoffTime")
    List<Payment> findStalePendingPayments(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Tìm thanh toán theo status với phân trang
     */
    Page<Payment> findByStatus(Payment.PaymentStatus status, Pageable pageable);

    /**
     * Tìm thanh toán trong khoảng thời gian
     */
    @Query("SELECT p FROM Payment p WHERE p.createdAt BETWEEN :startDate AND :endDate ORDER BY p.createdAt DESC")
    List<Payment> findPaymentsByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Tính tổng tiền thanh toán của một bệnh nhân
     */
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.patient.id = :patientId AND p.status = 'COMPLETED'")
    BigDecimal getTotalAmountByPatient(@Param("patientId") Long patientId);

    /**
     * Đếm số appointment đã thanh toán của bác sĩ
     */
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.doctor.id = :doctorId AND p.status = 'COMPLETED'")
    long countCompletedPaymentsByDoctor(@Param("doctorId") Long doctorId);

    /**
     * Tìm thanh toán theo nhiều trạng thái
     */
    @Query("SELECT p FROM Payment p WHERE p.status IN :statuses ORDER BY p.createdAt DESC")
    List<Payment> findByStatusIn(@Param("statuses") List<Payment.PaymentStatus> statuses);

    /**
     * Thống kê thanh toán theo ngày trong tháng
     */
    @Query("SELECT DAY(p.createdAt) as day, COUNT(p) as count, SUM(p.amount) as total " +
            "FROM Payment p WHERE p.status = 'COMPLETED' AND YEAR(p.createdAt) = :year AND MONTH(p.createdAt) = :month " +
            "GROUP BY DAY(p.createdAt) ORDER BY DAY(p.createdAt)")
    List<Object[]> getDailyStatistics(@Param("year") int year, @Param("month") int month);

    /**
     * Thống kê thanh toán theo phương thức trong khoảng thời gian
     */
    @Query("SELECT p.paymentMethod, COUNT(p) as count, SUM(p.amount) as total " +
            "FROM Payment p WHERE p.status = 'COMPLETED' AND p.createdAt BETWEEN :startDate AND :endDate " +
            "GROUP BY p.paymentMethod")
    List<Object[]> getPaymentMethodStatistics(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Lấy thanh toán gần đây nhất của bệnh nhân
     */
    @Query("SELECT p FROM Payment p WHERE p.patient.id = :patientId ORDER BY p.createdAt DESC")
    List<Payment> findRecentPaymentsByPatient(@Param("patientId") Long patientId, Pageable pageable);

    /**
     * Tìm thanh toán có transaction ID
     */
    @Query("SELECT p FROM Payment p WHERE p.transactionId = :transactionId")
    Optional<Payment> findByTransactionId(@Param("transactionId") String transactionId);

    /**
     * Đếm thanh toán failed trong ngày
     */
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = 'FAILED' AND DATE(p.createdAt) = DATE(:date)")
    long countFailedPaymentsToday(@Param("date") LocalDateTime date);

    /**
     * Lấy doanh thu trung bình theo tháng
     */
    @Query("SELECT AVG(monthly_totals.monthly_revenue) FROM (" +
            "SELECT SUM(p.amount) as monthly_revenue FROM Payment p " +
            "WHERE p.status = 'COMPLETED' AND YEAR(p.createdAt) = :year " +
            "GROUP BY MONTH(p.createdAt)) as monthly_totals")
    Double getAverageMonthlyRevenue(@Param("year") int year);

    /**
     * Tìm thanh toán duplicate (cùng appointment, cùng amount)
     */
    @Query("SELECT p FROM Payment p WHERE p.appointment.id = :appointmentId AND p.amount = :amount AND p.status = 'COMPLETED'")
    List<Payment> findDuplicatePayments(@Param("appointmentId") Long appointmentId, @Param("amount") BigDecimal amount);

    /**
     * Lấy top appointment có giá trị cao nhất
     */
    @Query("SELECT p.appointment.id, p.amount FROM Payment p WHERE p.status = 'COMPLETED' ORDER BY p.amount DESC")
    List<Object[]> findHighestValueAppointments(Pageable pageable);

    /**
     * Thống kê thanh toán theo giờ trong ngày
     */
    @Query("SELECT HOUR(p.createdAt) as hour, COUNT(p) as count " +
            "FROM Payment p WHERE p.status = 'COMPLETED' AND DATE(p.createdAt) = DATE(:date) " +
            "GROUP BY HOUR(p.createdAt) ORDER BY HOUR(p.createdAt)")
    List<Object[]> getHourlyStatistics(@Param("date") LocalDateTime date);

    /**
     * Tìm bệnh nhân có tổng thanh toán cao nhất
     */
    @Query("SELECT p.patient, SUM(p.amount) as totalAmount " +
            "FROM Payment p WHERE p.status = 'COMPLETED' " +
            "GROUP BY p.patient ORDER BY totalAmount DESC")
    List<Object[]> findTopSpendingPatients(Pageable pageable);

    /**
     * Đếm số lượng refund
     */
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.amount < 0")
    long countRefunds();

    /**
     * Tổng tiền refund
     */
    @Query("SELECT SUM(ABS(p.amount)) FROM Payment p WHERE p.amount < 0")
    BigDecimal getTotalRefundAmount();


    /**
     * Tìm kiếm payment theo tên bệnh nhân hoặc bác sĩ
     */
    @Query("SELECT p FROM Payment p " +
            "JOIN p.patient pt JOIN p.doctor d " +
            "WHERE LOWER(pt.fullName) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(d.fullName) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR p.transactionId LIKE CONCAT('%', :query, '%')")
    Page<Payment> searchByPatientOrDoctorName(@Param("query") String query, Pageable pageable);

    /**
     * Lấy payments trong khoảng thời gian với status
     */
    @Query("SELECT p FROM Payment p WHERE p.createdAt BETWEEN :startDate AND :endDate AND p.status = :status")
    List<Payment> findByDateRangeAndStatus(@Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate,
                                           @Param("status") Payment.PaymentStatus status);

    /**
     * Đếm payments theo status trong khoảng thời gian
     */
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.createdAt BETWEEN :startDate AND :endDate AND p.status = :status")
    long countByDateRangeAndStatus(@Param("startDate") LocalDateTime startDate,
                                   @Param("endDate") LocalDateTime endDate,
                                   @Param("status") Payment.PaymentStatus status);

    /**
     * Lấy payment gần đây nhất của appointment
     */
    @Query("SELECT p FROM Payment p WHERE p.appointment.id = :appointmentId ORDER BY p.createdAt DESC")
    List<Payment> findRecentByAppointmentId(@Param("appointmentId") Long appointmentId, Pageable pageable);

    /**
     * Tìm payments có amount trong khoảng
     */
    @Query("SELECT p FROM Payment p WHERE p.amount BETWEEN :minAmount AND :maxAmount AND p.status = 'COMPLETED'")
    List<Payment> findByAmountRange(@Param("minAmount") BigDecimal minAmount, @Param("maxAmount") BigDecimal maxAmount);

    /**
     * Thống kê theo tuần
     */
    @Query("SELECT WEEK(p.createdAt) as week, COUNT(p) as count, SUM(p.amount) as total " +
            "FROM Payment p WHERE p.status = 'COMPLETED' AND YEAR(p.createdAt) = :year " +
            "GROUP BY WEEK(p.createdAt) ORDER BY WEEK(p.createdAt)")
    List<Object[]> getWeeklyStatistics(@Param("year") int year);
}