package com.hospital.telemedicine.controller;

import com.hospital.telemedicine.dto.request.ChatMessageRequest;
import com.hospital.telemedicine.dto.response.ChatMessageResponse;
import com.hospital.telemedicine.dto.UserStatusDTO;
import com.hospital.telemedicine.service.ChatService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

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
        chatService.updateUserStatus(statusRequest);
        messagingTemplate.convertAndSend("/topic/user-status",
                chatService.getUserStatus(statusRequest.getUserId()));
    }

    @MessageMapping("/chat.read")
    public void markAsRead(@Payload Long messageId) {
        chatService.updateMessageStatus(messageId, true);
        // Gửi cập nhật trạng thái đọc nếu cần
    }
}