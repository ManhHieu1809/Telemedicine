
package com.hospital.telemedicine.service;

import com.hospital.telemedicine.entity.Payment;
import com.hospital.telemedicine.entity.User;
import com.hospital.telemedicine.entity.Notification;
import com.hospital.telemedicine.repository.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class PaymentNotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private JavaMailSender mailSender;

    /**
     * Gửi thông báo thanh toán thành công
     */
    public void sendPaymentSuccessNotification(Payment payment) {
        try {
            // Thông báo cho bệnh nhân
            createNotification(payment.getPatient().getUser(),
                    "Thanh toán thành công",
                    String.format("Thanh toán %,.0f VND cho lịch hẹn #%d đã được xử lý thành công.",
                            payment.getAmount().doubleValue(), payment.getAppointment().getId()));

            // Thông báo cho bác sĩ
            createNotification(payment.getDoctor().getUser(),
                    "Thanh toán nhận được",
                    String.format("Bạn đã nhận được thanh toán %,.0f VND từ bệnh nhân %s cho lịch hẹn #%d.",
                            payment.getAmount().doubleValue(),
                            payment.getPatient().getFullName(),
                            payment.getAppointment().getId()));

            // Gửi email xác nhận
            sendPaymentConfirmationEmail(payment);

        } catch (Exception e) {
            log.error("Error sending payment success notification: ", e);
        }
    }

    /**
     * Gửi thông báo thanh toán thất bại
     */
    public void sendPaymentFailureNotification(Payment payment, String reason) {
        try {
            createNotification(payment.getPatient().getUser(),
                    "Thanh toán thất bại",
                    String.format("Thanh toán %,.0f VND cho lịch hẹn #%d đã thất bại. Lý do: %s. Vui lòng thử lại.",
                            payment.getAmount().doubleValue(),
                            payment.getAppointment().getId(),
                            reason));

            // Gửi email thông báo thất bại
            sendPaymentFailureEmail(payment, reason);

        } catch (Exception e) {
            log.error("Error sending payment failure notification: ", e);
        }
    }

    /**
     * Gửi thông báo hoàn tiền
     */
    public void sendRefundNotification(Payment originalPayment, Payment refundPayment) {
        try {
            createNotification(originalPayment.getPatient().getUser(),
                    "Hoàn tiền",
                    String.format("Hoàn tiền %,.0f VND cho lịch hẹn #%d đã được xử lý thành công.",
                            refundPayment.getAmount().abs().doubleValue(),
                            originalPayment.getAppointment().getId()));

            // Gửi email xác nhận hoàn tiền
            sendRefundConfirmationEmail(originalPayment, refundPayment);

        } catch (Exception e) {
            log.error("Error sending refund notification: ", e);
        }
    }

    /**
     * Gửi email xác nhận thanh toán
     */
    private void sendPaymentConfirmationEmail(Payment payment) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(payment.getPatient().getUser().getEmail());
            helper.setSubject("Xác nhận thanh toán - Telemedicine");

            String emailContent = buildPaymentConfirmationEmailContent(payment);
            helper.setText(emailContent, true);

            mailSender.send(message);
            log.info("Payment confirmation email sent to: {}", payment.getPatient().getUser().getEmail());

        } catch (Exception e) {
            log.error("Error sending payment confirmation email: ", e);
        }
    }

    /**
     * Gửi email thông báo thanh toán thất bại
     */
    private void sendPaymentFailureEmail(Payment payment, String reason) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(payment.getPatient().getUser().getEmail());
            helper.setSubject("Thanh toán thất bại - Telemedicine");

            String emailContent = buildPaymentFailureEmailContent(payment, reason);
            helper.setText(emailContent, true);

            mailSender.send(message);

        } catch (Exception e) {
            log.error("Error sending payment failure email: ", e);
        }
    }

    /**
     * Gửi email xác nhận hoàn tiền
     */
    private void sendRefundConfirmationEmail(Payment originalPayment, Payment refundPayment) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(originalPayment.getPatient().getUser().getEmail());
            helper.setSubject("Xác nhận hoàn tiền - Telemedicine");

            String emailContent = buildRefundConfirmationEmailContent(originalPayment, refundPayment);
            helper.setText(emailContent, true);

            mailSender.send(message);

        } catch (Exception e) {
            log.error("Error sending refund confirmation email: ", e);
        }
    }

    /**
     * Tạo notification trong database
     */
    private void createNotification(User user, String title, String message) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setMessage(title + ": " + message);
        notification.setStatus(Notification.Status.UNREAD);
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    /**
     * Build email content cho payment confirmation
     */
    private String buildPaymentConfirmationEmailContent(Payment payment) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #7CB342;">Xác nhận thanh toán thành công</h2>
                    
                    <p>Kính chào %s,</p>
                    
                    <p>Cảm ơn bạn đã thanh toán. Dưới đây là thông tin chi tiết:</p>
                    
                    <div style="background-color: #f9f9f9; padding: 20px; border-radius: 5px; margin: 20px 0;">
                        <h3 style="margin-top: 0; color: #7CB342;">Thông tin thanh toán</h3>
                        <table style="width: 100%%; border-collapse: collapse;">
                            <tr>
                                <td style="padding: 8px 0; font-weight: bold;">Mã thanh toán:</td>
                                <td style="padding: 8px 0;">#%d</td>
                            </tr>
                            <tr>
                                <td style="padding: 8px 0; font-weight: bold;">Lịch hẹn:</td>
                                <td style="padding: 8px 0;">#%d</td>
                            </tr>
                            <tr>
                                <td style="padding: 8px 0; font-weight: bold;">Bác sĩ:</td>
                                <td style="padding: 8px 0;">%s</td>
                            </tr>
                            <tr>
                                <td style="padding: 8px 0; font-weight: bold;">Số tiền:</td>
                                <td style="padding: 8px 0; color: #7CB342; font-size: 18px; font-weight: bold;">%,.0f VND</td>
                            </tr>
                            <tr>
                                <td style="padding: 8px 0; font-weight: bold;">Phương thức:</td>
                                <td style="padding: 8px 0;">%s</td>
                            </tr>
                            <tr>
                                <td style="padding: 8px 0; font-weight: bold;">Thời gian:</td>
                                <td style="padding: 8px 0;">%s</td>
                            </tr>
                            %s
                        </table>
                    </div>
                    
                    <p>Lịch hẹn của bạn đã được xác nhận. Bạn sẽ nhận được thông báo nhắc nhở trước giờ hẹn.</p>
                    
                    <p>Nếu có bất kỳ thắc mắc nào, vui lòng liên hệ với chúng tôi.</p>
                    
                    <p style="margin-top: 30px;">
                        Trân trọng,<br>
                        <strong>Đội ngũ Telemedicine</strong>
                    </p>
                    
                    <hr style="margin: 30px 0; border: none; border-top: 1px solid #eee;">
                    <p style="font-size: 12px; color: #666;">
                        Email này được gửi tự động. Vui lòng không trả lời email này.
                    </p>
                </div>
            </body>
            </html>
            """,
                payment.getPatient().getFullName(),
                payment.getId(),
                payment.getAppointment().getId(),
                payment.getDoctor().getFullName(),
                payment.getAmount().doubleValue(),
                payment.getPaymentMethod().toString(),
                payment.getCreatedAt().format(formatter),
                payment.getTransactionId() != null ?
                        String.format("<tr><td style=\"padding: 8px 0; font-weight: bold;\">Mã giao dịch:</td><td style=\"padding: 8px 0;\">%s</td></tr>", payment.getTransactionId()) :
                        ""
        );
    }

    /**
     * Build email content cho payment failure
     */
    private String buildPaymentFailureEmailContent(Payment payment, String reason) {
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #e74c3c;">Thanh toán không thành công</h2>
                    
                    <p>Kính chào %s,</p>
                    
                    <p>Rất tiếc, thanh toán của bạn không thể được xử lý.</p>
                    
                    <div style="background-color: #f9f9f9; padding: 20px; border-radius: 5px; margin: 20px 0;">
                        <h3 style="margin-top: 0; color: #e74c3c;">Thông tin giao dịch</h3>
                        <p><strong>Lịch hẹn:</strong> #%d</p>
                        <p><strong>Số tiền:</strong> %,.0f VND</p>
                        <p><strong>Lý do:</strong> %s</p>
                    </div>
                    
                    <p>Vui lòng thử lại hoặc liên hệ với chúng tôi để được hỗ trợ.</p>
                    
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="http://localhost:3000/appointments/%d/payment" 
                           style="background-color: #7CB342; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px;">
                            Thử lại thanh toán
                        </a>
                    </div>
                    
                    <p style="margin-top: 30px;">
                        Trân trọng,<br>
                        <strong>Đội ngũ Telemedicine</strong>
                    </p>
                </div>
            </body>
            </html>
            """,
                payment.getPatient().getFullName(),
                payment.getAppointment().getId(),
                payment.getAmount().doubleValue(),
                reason,
                payment.getAppointment().getId()
        );
    }

    /**
     * Build email content cho refund confirmation
     */
    private String buildRefundConfirmationEmailContent(Payment originalPayment, Payment refundPayment) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #7CB342;">Xác nhận hoàn tiền</h2>
                    
                    <p>Kính chào %s,</p>
                    
                    <p>Hoàn tiền của bạn đã được xử lý thành công.</p>
                    
                    <div style="background-color: #f9f9f9; padding: 20px; border-radius: 5px; margin: 20px 0;">
                        <h3 style="margin-top: 0; color: #7CB342;">Thông tin hoàn tiền</h3>
                        <p><strong>Lịch hẹn gốc:</strong> #%d</p>
                        <p><strong>Số tiền hoàn:</strong> <span style="color: #7CB342; font-size: 18px; font-weight: bold;">%,.0f VND</span></p>
                        <p><strong>Thời gian xử lý:</strong> %s</p>
                    </div>
                    
                    <p>Số tiền sẽ được hoàn về tài khoản/thẻ của bạn trong vòng 3-5 ngày làm việc.</p>
                    
                    <p style="margin-top: 30px;">
                        Trân trọng,<br>
                        <strong>Đội ngũ Telemedicine</strong>
                    </p>
                </div>
            </body>
            </html>
            """,
                originalPayment.getPatient().getFullName(),
                originalPayment.getAppointment().getId(),
                refundPayment.getAmount().abs().doubleValue(),
                refundPayment.getCreatedAt().format(formatter)
        );
    }
}