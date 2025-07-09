package com.hospital.telemedicine.controller;

import com.hospital.telemedicine.dto.request.ChatMessageRequest;
import com.hospital.telemedicine.dto.request.MessageReadRequest;
import com.hospital.telemedicine.dto.response.ChatMessageResponse;
import com.hospital.telemedicine.dto.UserStatusDTO;
import com.hospital.telemedicine.dto.response.MessageReadResponse;
import com.hospital.telemedicine.entity.Message;
import com.hospital.telemedicine.entity.User;
import com.hospital.telemedicine.repository.UserRepository;
import com.hospital.telemedicine.service.ChatService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class ChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;
    @Autowired
    private UserRepository userRepository;
    @Value("${app.jwt.secret}")
    private String jwtSecret;
    private final Map<String, Long> sessionUserMap = new ConcurrentHashMap<>();
    public ChatController(ChatService chatService, SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessageRequest messageRequest) {
        try {
            ChatMessageResponse response = chatService.saveMessage(messageRequest);
            String receiverUserId = messageRequest.getReceiverId().toString();
            String senderUserId = messageRequest.getSenderId().toString();
            messagingTemplate.convertAndSend("/queue/user-" + receiverUserId, response);
            messagingTemplate.convertAndSend("/queue/user-" + senderUserId, response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @MessageMapping("/chat.status")
    public void updateStatus(@Payload UserStatusDTO statusRequest) {
        try {
            // Validate input
            if (statusRequest.getUserId() == null || statusRequest.getStatus() == null) {
                System.err.println("Invalid status request: missing userId or status");
                return;
            }
            chatService.updateUserStatus(statusRequest);
            UserStatusDTO updatedStatus = chatService.getUserStatus(statusRequest.getUserId());
            messagingTemplate.convertAndSend("/topic/user-status", updatedStatus);
            messagingTemplate.convertAndSendToUser(
                    statusRequest.getUserId().toString(),
                    "/queue/status-update-confirm",
                    updatedStatus
            );

        } catch (Exception e) {
            System.err.println("Error updating status: " + e.getMessage());
            UserStatusDTO errorResponse = new UserStatusDTO();
            errorResponse.setUserId(statusRequest.getUserId());
            errorResponse.setStatus("ERROR");

            messagingTemplate.convertAndSendToUser(
                    statusRequest.getUserId().toString(),
                    "/queue/status-error",
                    errorResponse
            );
        }
    }

    @MessageMapping("/chat.read")
    public void markAsRead(@Payload MessageReadRequest readRequest) {
        MessageReadResponse readResponse = chatService.updateMessageStatus(
                readRequest.getMessageId(), true);
        Message message = chatService.getMessageById(readRequest.getMessageId());
        messagingTemplate.convertAndSendToUser(
                message.getSender().getId().toString(),
                "/queue/message-read",
                readResponse
        );
        messagingTemplate.convertAndSendToUser(
                readRequest.getReaderId().toString(),
                "/queue/message-read-confirm",
                readResponse
        );
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        try {
            StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
            String sessionId = headerAccessor.getSessionId();

            System.out.println("WebSocket connected - SessionId: " + sessionId);

            // Lấy user info từ session attributes (đã được set bởi WebSocket authentication)
            Long userId = (Long) headerAccessor.getSessionAttributes().get("userId");
            String username = (String) headerAccessor.getSessionAttributes().get("username");

            if (userId != null && username != null) {
                // Lưu mapping session -> userId
                sessionUserMap.put(sessionId, userId);

                // Cập nhật trạng thái user thành ONLINE
                try {
                    UserStatusDTO statusRequest = new UserStatusDTO();
                    statusRequest.setUserId(userId);
                    statusRequest.setStatus("ONLINE");

                    // Gọi method từ ChatService thay vì updateStatus
                    chatService.updateUserStatus(statusRequest);

                    System.out.println(" User " + username + " (ID: " + userId + ") connected via WebSocket and set to ONLINE");

                    // Gửi thông báo đến các user khác về việc user này đã online
                    UserStatusNotification notification = new UserStatusNotification();
                    notification.setUserId(userId);
                    notification.setUsername(username);
                    notification.setStatus("ONLINE");
                    notification.setTimestamp(new Date());

                    messagingTemplate.convertAndSend("/topic/user-status", notification);

                } catch (Exception e) {
                    System.err.println(" Error updating user status to ONLINE: " + e.getMessage());
                    e.printStackTrace();
                }

            } else {
                System.err.println("User info not found in session attributes");
                System.err.println("Available session attributes: " + headerAccessor.getSessionAttributes());
            }

        } catch (Exception e) {
            System.err.println(" Error handling WebSocket connect: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        try {
            StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
            String sessionId = headerAccessor.getSessionId();

            System.out.println("WebSocket disconnected - SessionId: " + sessionId);

            // Lấy userId từ session mapping
            Long userId = sessionUserMap.get(sessionId);

            if (userId != null) {
                // Lấy username từ session attributes (nếu còn available)
                String username = (String) headerAccessor.getSessionAttributes().get("username");
                if (username == null) {
                    username = "Unknown";
                }

                // Xóa mapping
                sessionUserMap.remove(sessionId);

                // Kiểm tra xem user có session nào khác đang active không
                boolean hasOtherSessions = sessionUserMap.values().contains(userId);

                if (!hasOtherSessions) {
                    // Chỉ set OFFLINE nếu user không có session nào khác
                    try {
                        UserStatusDTO statusRequest = new UserStatusDTO();
                        statusRequest.setUserId(userId);
                        statusRequest.setStatus("OFFLINE");

                        // Gọi method từ ChatService thay vì updateStatus
                        chatService.updateUserStatus(statusRequest);

                        System.out.println(" User " + username + " (ID: " + userId + ") disconnected and set to OFFLINE");

                        // Gửi thông báo đến các user khác về việc user này đã offline
                        UserStatusNotification notification = new UserStatusNotification();
                        notification.setUserId(userId);
                        notification.setUsername(username);
                        notification.setStatus("OFFLINE");
                        notification.setTimestamp(new Date());

                        messagingTemplate.convertAndSend("/topic/user-status", notification);

                    } catch (Exception e) {
                        System.err.println(" Error updating user status to OFFLINE: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    System.out.println(" User " + username + " (ID: " + userId + ") still has other active sessions");
                }
            } else {
                System.err.println("No userId found for disconnected session: " + sessionId);
                System.err.println("Available session attributes: " + headerAccessor.getSessionAttributes());
            }

        } catch (Exception e) {
            System.err.println(" Error handling WebSocket disconnect: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static class UserStatusNotification {
        private Long userId;
        private String username;
        private String status;
        private Date timestamp;

        // Getters and setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public Date getTimestamp() { return timestamp; }
        public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
    }

}