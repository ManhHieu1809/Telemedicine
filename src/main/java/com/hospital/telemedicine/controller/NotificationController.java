package com.hospital.telemedicine.controller;

import com.hospital.telemedicine.entity.Notification;
import com.hospital.telemedicine.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping("/{userId}")
    public List<Notification> getNotifications(@PathVariable Long userId) {
        return notificationService.getNotifications(userId);
    }

    @PostMapping("/markAsRead/{notificationId}")
    public void markAsRead(@PathVariable Long notificationId) {
        notificationService.markNotificationAsRead(notificationId);
    }
}