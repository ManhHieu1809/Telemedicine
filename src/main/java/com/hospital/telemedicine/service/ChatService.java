package com.hospital.telemedicine.service;

import com.hospital.telemedicine.dto.request.ChatMessageRequest;
import com.hospital.telemedicine.dto.response.ChatMessageResponse;
import com.hospital.telemedicine.dto.ConversationDTO;
import com.hospital.telemedicine.dto.MessageDTO;
import com.hospital.telemedicine.dto.UserStatusDTO;
import com.hospital.telemedicine.entity.Conversation;
import com.hospital.telemedicine.entity.Message;
import com.hospital.telemedicine.entity.User;
import com.hospital.telemedicine.repository.ConversationRepository;
import com.hospital.telemedicine.repository.MessageRepository;
import com.hospital.telemedicine.repository.UserRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

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
                    return new ConversationDTO(
                            conv.getId(),
                            conv.getParticipants().stream().map(User::getId).collect(Collectors.toList()),
                            conv.getLastMessage(),
                            conv.getLastActive(),
                            messages.stream().map(this::mapToMessageDTO).collect(Collectors.toList())
                    );
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateMessageStatus(Long messageId, boolean isRead) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tin nhắn"));
        message.setRead(isRead);
        messageRepository.save(message);
    }

    public void updateUserStatus(UserStatusDTO request) {
        System.out.println("Updating status for user " + request.getUserId() + " to " + request.getStatus());
    }

    public UserStatusDTO getUserStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
        UserStatusDTO status = new UserStatusDTO();
        status.setUserId(userId);
        status.setStatus("ONLINE");
        status.setLastActive(LocalDateTime.now());
        status.setUsername(user.getUsername());
        return status;
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