package com.hospital.telemedicine.service;

import com.hospital.telemedicine.dto.request.ChatMessageRequest;
import com.hospital.telemedicine.dto.response.ChatMessageResponse;
import com.hospital.telemedicine.dto.ConversationDTO;
import com.hospital.telemedicine.dto.MessageDTO;
import com.hospital.telemedicine.dto.UserStatusDTO;
import com.hospital.telemedicine.dto.response.MessageReadResponse;
import com.hospital.telemedicine.entity.Conversation;
import com.hospital.telemedicine.entity.Message;
import com.hospital.telemedicine.entity.User;
import com.hospital.telemedicine.entity.UserStatus;
import com.hospital.telemedicine.repository.ConversationRepository;
import com.hospital.telemedicine.repository.MessageRepository;
import com.hospital.telemedicine.repository.UserRepository;
import com.hospital.telemedicine.repository.UserStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;
    @Autowired
    private UserStatusRepository userStatusRepository;
    public ChatService(MessageRepository messageRepository,
                       ConversationRepository conversationRepository,
                       UserRepository userRepository,
                       JdbcTemplate jdbcTemplate) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public ChatMessageResponse saveMessage(ChatMessageRequest request) {
        User sender = userRepository.findById(request.getSenderId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người gửi"));
        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người nhận"));

        Conversation conversation = getOrCreateConversation(sender.getId(), receiver.getId());
        conversation.setLastMessage(request.getContent());
        conversation.setLastActive(request.getTimestamp() != null ? request.getTimestamp() : LocalDateTime.now());
        conversationRepository.save(conversation);

        Message message = new Message();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent(request.getContent());
        message.setSentAt(request.getTimestamp() != null ? request.getTimestamp() : LocalDateTime.now());

        message = messageRepository.save(message);

        // Cập nhật conversation_id trong DB trực tiếp
        jdbcTemplate.update("UPDATE chat_messages SET conversation = ? WHERE id = ?",
                conversation.getId(), message.getId());

        return mapToMessageResponse(message);
    }

    private Conversation getOrCreateConversation(Long userId1, Long userId2) {
        User user1 = userRepository.findById(userId1)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
        User user2 = userRepository.findById(userId2)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        List<Conversation> existingConversations = conversationRepository.findByParticipantsContaining(userId1);
        return existingConversations.stream()
                .filter(conv -> conv.getParticipants().stream().anyMatch(p -> p.getId().equals(userId2)))
                .findFirst()
                .orElseGet(() -> {
                    Conversation newConv = new Conversation();
                    newConv.setParticipants(List.of(user1, user2));
                    newConv.setIsGroup(false);
                    return conversationRepository.save(newConv);
                });
    }

    public List<ChatMessageResponse> getChatHistory(Long userId1, Long userId2) {
        List<Message> messages = messageRepository.findChatHistory(userId1, userId2);
        return messages.stream()
                .map(this::mapToMessageResponse)
                .collect(Collectors.toList());
    }

    public List<ConversationDTO> getConversations(Long userId) {
        List<Conversation> conversations = conversationRepository.findByParticipantsContaining(userId);
        return conversations.stream()
                .map(conv -> {
                    List<Message> messages = messageRepository.findByConversationId(conv.getId());


                    Long unreadCount = messageRepository.countUnreadMessagesByConversationAndUser(
                            conv.getId(), userId);

                    ConversationDTO dto = new ConversationDTO(
                            conv.getId(),
                            conv.getParticipants().stream().map(User::getId).collect(Collectors.toList()),
                            conv.getLastMessage(),
                            conv.getLastActive(),
                            messages.stream().map(this::mapToMessageDTO).collect(Collectors.toList())
                    );


                    dto.setUnreadCount(unreadCount.intValue());


                    Optional<Message> latestMessage = messageRepository.findLatestMessageByConversationId(conv.getId());
                    if (latestMessage.isPresent()) {
                        dto.setLastMessageTime(latestMessage.get().getSentAt());
                    } else {
                        
                        dto.setLastMessageTime(conv.getLastActive());
                    }

                    // Tìm người chat khác (không phải current user)
                    User otherUser = conv.getParticipants().stream()
                            .filter(participant -> !participant.getId().equals(userId))
                            .findFirst()
                            .orElse(null);

                    // Set thông tin người chat khác
                    if (otherUser != null) {
                        dto.setOtherUserId(otherUser.getId());
                        dto.setOtherUserName(otherUser.getUsername());
                        dto.setOtherUserAvatar(otherUser.getAvatarUrl());

                        // **LOGIC THỰC TẾ CHO STATUS**
                        UserStatus userStatus = userStatusRepository.findById(otherUser.getId()).orElse(null);
                        if (userStatus != null) {
                            if (userStatus.isOnline()) {
                                dto.setOtherUserStatus("ONLINE");
                                dto.setOtherUserLastSeen(null); // Đang online nên không cần lastSeen
                            } else {
                                dto.setOtherUserStatus("OFFLINE");
                                dto.setOtherUserLastSeen(userStatus.getLastSeen());
                            }
                        } else {
                            // Nếu chưa có record trong user_status, tạo mặc định
                            dto.setOtherUserStatus("OFFLINE");
                            dto.setOtherUserLastSeen(LocalDateTime.now());
                        }
                    }

                    return dto;
                })
                // **THÊM SORT THEO THỜI GIAN TIN NHẮN MỚI NHẤT**
                .sorted((a, b) -> b.getLastMessageTime().compareTo(a.getLastMessageTime()))
                .collect(Collectors.toList());
    }

    @Transactional
    public MessageReadResponse updateMessageStatus(Long messageId, boolean isRead) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tin nhắn"));

        message.setRead(isRead);
        message = messageRepository.save(message);

        // Lấy thông tin người đọc
        User reader = message.getReceiver();

        // Tạo response với avatar
        MessageReadResponse response = new MessageReadResponse();
        response.setMessageId(messageId);
        response.setReadBy(reader.getId());
        response.setReadByAvatar(reader.getAvatarUrl()); // Avatar của người đọc
        response.setReadAt(LocalDateTime.now());
        response.setStatus("READ");

        return response;
    }

    @Transactional
    public void updateUserStatus(UserStatusDTO request) {
        try {
            // Tìm hoặc tạo UserStatus
            UserStatus userStatus = userStatusRepository.findById(request.getUserId())
                    .orElseGet(() -> {
                        UserStatus newStatus = new UserStatus();
                        newStatus.setUserId(request.getUserId());
                        User user = userRepository.findById(request.getUserId())
                                .orElseThrow(() -> new IllegalArgumentException("User không tồn tại"));
                        newStatus.setUser(user);
                        return newStatus;
                    });

            // Cập nhật status
            LocalDateTime now = LocalDateTime.now();
            if ("ONLINE".equals(request.getStatus())) {
                userStatus.setOnline(true);
                userStatus.setLastSeen(now);
            } else if ("OFFLINE".equals(request.getStatus())) {
                userStatus.setOnline(false);
                userStatus.setLastSeen(now);
            }

            // Lưu vào database
            userStatusRepository.save(userStatus);

            System.out.println("Updated status for user " + request.getUserId() + " to " + request.getStatus() + " at " + now);

        } catch (Exception e) {
            System.err.println("Error updating user status: " + e.getMessage());
            throw new RuntimeException("Không thể cập nhật trạng thái người dùng", e);
        }
    }

    public UserStatusDTO getUserStatus(Long userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

            UserStatusDTO status = new UserStatusDTO();
            status.setUserId(userId);
            status.setUsername(user.getUsername());

            // Lấy status thực từ database
            UserStatus userStatus = userStatusRepository.findById(userId).orElse(null);

            if (userStatus != null) {
                // Kiểm tra xem có thực sự online không (trong vòng 5 phút)
                LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);

                if (userStatus.isOnline() && userStatus.getLastSeen().isAfter(fiveMinutesAgo)) {
                    status.setStatus("ONLINE");
                    status.setLastActive(userStatus.getLastSeen());
                } else {
                    status.setStatus("OFFLINE");
                    status.setLastActive(userStatus.getLastSeen());

                    // Tự động set offline nếu quá 5 phút không hoạt động
                    if (userStatus.isOnline()) {
                        userStatus.setOnline(false);
                        userStatusRepository.save(userStatus);
                    }
                }
            } else {
                // Chưa có record, tạo mặc định
                status.setStatus("OFFLINE");
                status.setLastActive(LocalDateTime.now());
            }

            return status;

        } catch (Exception e) {
            System.err.println("Error getting user status: " + e.getMessage());
            // Return default status
            UserStatusDTO defaultStatus = new UserStatusDTO();
            defaultStatus.setUserId(userId);
            defaultStatus.setStatus("OFFLINE");
            defaultStatus.setLastActive(LocalDateTime.now());
            return defaultStatus;
        }
    }

    public Message getMessageById(Long messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tin nhắn"));
    }

    @Transactional
    public List<MessageReadResponse> markConversationAsRead(Long conversationId, Long readerId) {
        List<Message> unreadMessages = messageRepository.findUnreadMessagesByConversationAndReceiver(
                conversationId, readerId);

        List<MessageReadResponse> responses = new ArrayList<>();
        for (Message message : unreadMessages) {
            message.setRead(true);
            messageRepository.save(message);

            MessageReadResponse response = new MessageReadResponse();
            response.setMessageId(message.getId());
            response.setReadBy(readerId);
            response.setReadAt(LocalDateTime.now());
            response.setStatus("READ");
            responses.add(response);
        }

        return responses;
    }

    private ChatMessageResponse mapToMessageResponse(Message message) {
        ChatMessageResponse response = new ChatMessageResponse();
        response.setId(message.getId());
        response.setSenderId(message.getSender().getId());
        response.setSenderName(message.getSender().getUsername());
        response.setReceiverId(message.getReceiver().getId());
        response.setReceiverName(message.getReceiver().getUsername());
        response.setContent(message.getContent());
        response.setTimestamp(message.getSentAt());
        response.setStatus(message.isRead() ? "READ" : "SENT");
        return response;
    }

    private MessageDTO mapToMessageDTO(Message message) {
        MessageDTO dto = new MessageDTO();
        dto.setId(message.getId());
        dto.setSenderId(message.getSender().getId());
        dto.setSenderName(message.getSender().getUsername());
        dto.setReceiverId(message.getReceiver().getId());
        dto.setReceiverName(message.getReceiver().getUsername());
        dto.setContent(message.getContent());
        dto.setTimestamp(message.getSentAt());
        dto.setRead(message.isRead());
        return dto;
    }
}