package com.hospital.telemedicine.controller;

import com.hospital.telemedicine.dto.response.ChatMessageResponse;
import com.hospital.telemedicine.dto.ConversationDTO;
import com.hospital.telemedicine.dto.UserStatusDTO;
import com.hospital.telemedicine.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatRestController {

    private final ChatService chatService;

    public ChatRestController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/history")
    public ResponseEntity<List<ChatMessageResponse>> getChatHistory(
            @RequestParam Long otherUserId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = extractUserId(userDetails);
        List<ChatMessageResponse> messages = chatService.getChatHistory(userId, otherUserId);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationDTO>> getConversations(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = extractUserId(userDetails);
        List<ConversationDTO> conversations = chatService.getConversations(userId);
        return ResponseEntity.ok(conversations);
    }

    @GetMapping("/status/{userId}")
    public ResponseEntity<UserStatusDTO> getUserStatus(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long currentUserId = extractUserId(userDetails);
        UserStatusDTO status = chatService.getUserStatus(userId);
        return ResponseEntity.ok(status);
    }

    @PostMapping("/status")
    public ResponseEntity<Void> updateUserStatus(
            @RequestBody UserStatusDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = extractUserId(userDetails);
        if (!userId.equals(request.getUserId())) {
            throw new SecurityException("Không thể cập nhật trạng thái cho người dùng khác");
        }
        chatService.updateUserStatus(request);
        return ResponseEntity.ok().build();
    }

    private Long extractUserId(UserDetails userDetails) {
        if (userDetails instanceof com.hospital.telemedicine.security.UserDetailsImpl) {
            return ((com.hospital.telemedicine.security.UserDetailsImpl) userDetails).getId();
        }
        throw new IllegalArgumentException("UserDetails must be an instance of UserDetailsImpl");
    }
}