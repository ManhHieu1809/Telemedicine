package com.hospital.telemedicine.service;

import com.hospital.telemedicine.dto.UserStatusDTO;
import com.hospital.telemedicine.entity.Message;
import com.hospital.telemedicine.entity.Notification;
import com.hospital.telemedicine.entity.User;
import com.hospital.telemedicine.repository.MessageRepository;
import com.hospital.telemedicine.repository.NotificationRepository;
import com.hospital.telemedicine.repository.UserRepository;
import com.hospital.telemedicine.repository.UserStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    public void sendNotification(Long userId, String message) {
        Notification notification = new Notification();
        User user = new User();
        user.setId(userId);
        notification.setUser(user);
        notification.setMessage(message);
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    public List<Notification> getNotifications(Long userId) {
        return notificationRepository.findByUserId(userId);
    }

    public void markNotificationAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId).orElseThrow();
        notification.setStatus(Notification.Status.READ);
        notificationRepository.save(notification);
    }
}