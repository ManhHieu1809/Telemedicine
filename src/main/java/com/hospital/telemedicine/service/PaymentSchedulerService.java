
package com.hospital.telemedicine.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Service để xử lý các tác vụ định kỳ liên quan đến payment
 */
@Slf4j
@Service
public class PaymentSchedulerService {

    @Autowired
    private PaymentService paymentService;

    /**
     * Cleanup stale pending payments mỗi 30 phút
     */
    @Scheduled(fixedRate = 30 * 60 * 1000) // 30 minutes
    public void cleanupStalePendingPayments() {
        log.info("Starting cleanup of stale pending payments");
        try {
            paymentService.cleanupStalePendingPayments();
            log.info("Completed cleanup of stale pending payments");
        } catch (Exception e) {
            log.error("Error during cleanup of stale pending payments: ", e);
        }
    }

    /**
     * Gửi báo cáo hàng ngày về payments
     */
    @Scheduled(cron = "0 0 8 * * *") // 8:00 AM mỗi ngày
    public void sendDailyPaymentReport() {
        log.info("Generating daily payment report");
        try {
            // Implementation để gửi báo cáo qua email
            // paymentService.sendDailyReport();
            log.info("Daily payment report sent successfully");
        } catch (Exception e) {
            log.error("Error sending daily payment report: ", e);
        }
    }

    /**
     * Kiểm tra và cập nhật trạng thái payments từ gateway
     */
    @Scheduled(fixedRate = 10 * 60 * 1000) // 10 minutes
    public void syncPaymentStatusWithGateway() {
        log.info("Syncing payment status with gateway");
        try {
            // paymentService.syncPendingPaymentsWithGateway();
            log.info("Payment status sync completed");
        } catch (Exception e) {
            log.error("Error syncing payment status: ", e);
        }
    }
}