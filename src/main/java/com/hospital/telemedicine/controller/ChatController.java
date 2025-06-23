package com.hospital.telemedicine.controller;

import com.hospital.telemedicine.dto.request.ChatMessageRequest;
import com.hospital.telemedicine.dto.request.MessageReadRequest;
import com.hospital.telemedicine.dto.response.ChatMessageResponse;
import com.hospital.telemedicine.dto.UserStatusDTO;
import com.hospital.telemedicine.dto.response.MessageReadResponse;
import com.hospital.telemedicine.entity.Message;
import com.hospital.telemedicine.service.ChatService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.LocalDateTime;

@Controller
public class ChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatController(ChatService chatService, SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessageRequest messageRequest) {
        ChatMessageResponse response = chatService.saveMessage(messageRequest);
        messagingTemplate.convertAndSendToUser(
                messageRequest.getReceiverId().toString(),
                "/queue/messages",
                response
        );
        messagingTemplate.convertAndSendToUser(
                messageRequest.getSenderId().toString(),
                "/queue/messages",
                response
        );
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
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        try {
            StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

            // Lấy userId từ session attributes hoặc header
            String sessionId = headerAccessor.getSessionId();
            System.out.println("WebSocket connected: " + sessionId);

//             Nếu có cách lấy userId từ JWT token hoặc session
//             String userId = getUserIdFromSession(headerAccessor);
//             if (userId != null) {
//                 UserStatusDTO statusRequest = new UserStatusDTO();
//                 statusRequest.setUserId(Long.parseLong(userId));
//                 statusRequest.setStatus("ONLINE");
//                 updateStatus(statusRequest);
//             }

        } catch (Exception e) {
            System.err.println("Error handling WebSocket connect: " + e.getMessage());
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        try {
            StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
            String sessionId = headerAccessor.getSessionId();
            System.out.println("WebSocket disconnected: " + sessionId);

            // Tương tự set OFFLINE
            // String userId = getUserIdFromSession(headerAccessor);
            // if (userId != null) {
            //     UserStatusDTO statusRequest = new UserStatusDTO();
            //     statusRequest.setUserId(Long.parseLong(userId));
            //     statusRequest.setStatus("OFFLINE");
            //     updateStatus(statusRequest);
            // }

        } catch (Exception e) {
            System.err.println("Error handling WebSocket disconnect: " + e.getMessage());
        }
    }
}