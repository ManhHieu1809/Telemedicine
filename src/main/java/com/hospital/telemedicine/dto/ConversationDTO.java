package com.hospital.telemedicine.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ConversationDTO {
    private Long id;
    private List<Long> participantIds;
    private String lastMessage;
    private LocalDateTime lastActive;
    private List<MessageDTO> messages;
    private int unreadCount;
    private boolean isGroup;

    // Thông tin về người chat khác (trong trường hợp chat 1-1)
    private Long otherUserId;
    private String otherUserName;
    private String otherUserAvatar;
    private String otherUserStatus; // ONLINE/OFFLINE
    private LocalDateTime otherUserLastSeen;

    public ConversationDTO() {}

    public ConversationDTO(Long id, List<Long> participantIds, String lastMessage, LocalDateTime lastActive, List<MessageDTO> messages) {
        this.id = id;
        this.participantIds = participantIds;
        this.lastMessage = lastMessage;
        this.lastActive = lastActive;
        this.messages = messages;
        this.unreadCount = 0;
        this.isGroup = false;
    }

    public ConversationDTO(Long id, List<Long> participantIds, String lastMessage, LocalDateTime lastActive,
                           List<MessageDTO> messages, int unreadCount, boolean isGroup) {
        this.id = id;
        this.participantIds = participantIds;
        this.lastMessage = lastMessage;
        this.lastActive = lastActive;
        this.messages = messages;
        this.unreadCount = unreadCount;
        this.isGroup = isGroup;
    }
}