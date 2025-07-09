package com.hospital.telemedicine.service;

import com.hospital.telemedicine.dto.DoctorDTO;
import com.hospital.telemedicine.dto.request.ChatMessageRequest;
import com.hospital.telemedicine.dto.response.ChatMessageResponse;
import com.hospital.telemedicine.dto.ConversationDTO;
import com.hospital.telemedicine.dto.MessageDTO;
import com.hospital.telemedicine.dto.UserStatusDTO;
import com.hospital.telemedicine.dto.response.MessageReadResponse;
import com.hospital.telemedicine.entity.*;
import com.hospital.telemedicine.repository.*;
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
    @Autowired
    private DoctorRepository doctorRepository;
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
        jdbcTemplate.update("UPDATE chat_messages SET conversation_id = ? WHERE id = ?",
                conversation.getId(), message.getId());

        return mapToMessageResponse(message);
    }

    @Transactional
    public ConversationDTO createOrGetConversationWithDoctor(Long patientId, Long doctorId) {
        try {
            User doctor = userRepository.findById(doctorId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bác sĩ"));

            if (!User.UserRole.DOCTOR.equals(doctor.getRoles())) {
                throw new IllegalArgumentException("User này không phải là bác sĩ");
            }
            User patient = userRepository.findById(patientId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bệnh nhân"));


            List<Conversation> existingConversations = conversationRepository.findByParticipantsContaining(patientId);
            Conversation conversation = existingConversations.stream()
                    .filter(conv -> conv.getParticipants().stream()
                            .anyMatch(p -> p.getId().equals(doctorId)))
                    .findFirst()
                    .orElse(null);


            if (conversation == null) {
                // Create conversation first without participants
                conversation = new Conversation();
                conversation.setIsGroup(false);
                conversation.setLastActive(LocalDateTime.now());
                conversation.setLastMessage(""); // Temporary empty message
                conversation.setCreatedAt(LocalDateTime.now());

                conversation = conversationRepository.save(conversation);

                jdbcTemplate.update(
                        "INSERT INTO conversation_participants (conversation_id, user_id) VALUES (?, ?)",
                        conversation.getId(), patientId
                );
                jdbcTemplate.update(
                        "INSERT INTO conversation_participants (conversation_id, user_id) VALUES (?, ?)",
                        conversation.getId(), doctorId
                );
                conversation = conversationRepository.findById(conversation.getId()).orElse(conversation);
                // Create welcome message
                createWelcomeMessage(conversation, patient, doctor);

            } else {
                conversation.setLastActive(LocalDateTime.now());
                conversation = conversationRepository.save(conversation);
            }

            // Convert to DTO
            ConversationDTO dto = new ConversationDTO();
            dto.setId(conversation.getId());
            dto.setOtherUserId(doctorId);
            dto.setOtherUserName(doctor.getUsername());
            dto.setLastActive(conversation.getLastActive());

            // Get last message
            List<Message> messages = messageRepository.findByConversationIdOrderBySentAt(conversation.getId());
            if (!messages.isEmpty()) {
                Message lastMessage = messages.get(messages.size() - 1);
                dto.setLastMessage(lastMessage.getContent());
                dto.setLastMessageTime(lastMessage.getSentAt());
            }

            // Count unread messages
            Long unreadCount = messageRepository.countUnreadMessagesByConversationAndUser(
                    conversation.getId(), patientId);
            dto.setUnreadCount(unreadCount != null ? unreadCount.intValue() : 0);

            // Get doctor status
            try {
                UserStatusDTO status = getUserStatus(doctorId);
                dto.setOtherUserStatus(status.getStatus());
            } catch (Exception e) {
                dto.setOtherUserStatus("OFFLINE");
            }

            return dto;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Không thể tạo cuộc hội thoại với bác sĩ", e);
        }
    }

    private void createWelcomeMessage(Conversation conversation, User patient, User doctor) {
        try {
            System.out.println(" Creating welcome message from doctor to patient");

            Message welcomeMessage = new Message();
            welcomeMessage.setSender(doctor);
            welcomeMessage.setReceiver(patient);
            welcomeMessage.setConversation(conversation); // IMPORTANT: Set conversation reference
            welcomeMessage.setContent("Chào " + (patient.getUsername()) +
                    "! Tôi là bác sĩ " + (doctor.getUsername()) +
                    ". Tôi có thể giúp gì cho bạn?");
            welcomeMessage.setSentAt(LocalDateTime.now());
            welcomeMessage.setRead(false);

            // Save message
            welcomeMessage = messageRepository.save(welcomeMessage);
            System.out.println(" Welcome message created with ID: " + welcomeMessage.getId());

            // Update conversation with last message
            conversation.setLastMessage(welcomeMessage.getContent());
            conversation.setLastActive(LocalDateTime.now());
            conversationRepository.save(conversation);

            System.out.println(" Conversation updated with welcome message");

        } catch (Exception e) {
            System.err.println(" Error creating welcome message: " + e.getMessage());
            e.printStackTrace();
            // Don't throw exception vì đây chỉ là feature phụ
        }
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

    public List<DoctorDTO> getAllDoctors() {
        try {
            List<User> doctors = userRepository.findByRole(User.UserRole.DOCTOR);
            return doctors.stream().map(doctor -> {
                DoctorDTO dto = new DoctorDTO();

                doctorRepository.findByUserId(doctor.getId()).ifPresent(doctorEntity -> {
                    dto.setId(doctorEntity.getId());
                });
                dto.setUserId(doctor.getId());
                dto.setUsername(doctor.getUsername());
                dto.setFullName((doctor.getUsername()));

                dto.setAvatarUrl(doctor.getAvatarUrl());

                // Lấy status
                try {
                    UserStatusDTO status = getUserStatus(doctor.getId());
                    dto.setStatus(status.getStatus());

                } catch (Exception e) {
                    dto.setStatus("OFFLINE");

                }

                return dto;
            }).collect(Collectors.toList());

        } catch (Exception e) {
            System.err.println(" Error getting all doctors: " + e.getMessage());
            throw new RuntimeException("Không thể lấy danh sách bác sĩ", e);
        }
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

                    // Lấy thông tin người chat khác
                    User otherUser = conv.getParticipants().stream()
                            .filter(participant -> !participant.getId().equals(userId))
                            .findFirst()
                            .orElse(null);

                    if (otherUser != null) {
                        dto.setOtherUserId(otherUser.getId());
                        dto.setOtherUserName(otherUser.getUsername());
                        dto.setOtherUserEmail(otherUser.getEmail());
                        dto.setOtherUserAvatar(otherUser.getAvatarUrl());

                        // Cập nhật status
                        UserStatus userStatus = userStatusRepository.findById(otherUser.getId()).orElse(null);
                        if (userStatus != null) {
                            if (userStatus.isOnline()) {
                                dto.setOtherUserStatus("ONLINE");
                                dto.setOtherUserLastSeen(null);
                            } else {
                                dto.setOtherUserStatus("OFFLINE");
                                dto.setOtherUserLastSeen(userStatus.getLastSeen());
                            }
                        } else {
                            dto.setOtherUserStatus("OFFLINE");
                            dto.setOtherUserLastSeen(LocalDateTime.now());
                        }
                    }

                    return dto;
                })
                .collect(Collectors.toList());
    }

    public List<ChatMessageResponse> getChatHistoryByConversation(Long conversationId, Long currentUserId) {
        try {

            Conversation conversation = conversationRepository.findById(conversationId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cuộc hội thoại"));

            boolean hasAccess = conversation.getParticipants().stream()
                    .anyMatch(participant -> participant.getId().equals(currentUserId));

            if (!hasAccess) {
                throw new SecurityException("Bạn không có quyền truy cập cuộc hội thoại này");
            }

            // Lấy messages theo conversationId
            List<Message> messages = messageRepository.findByConversationIdOrderBySentAt(conversationId);

            // Map to response DTOs
            List<ChatMessageResponse> responses = messages.stream()
                    .map(this::mapToMessageResponse)
                    .collect(Collectors.toList());

            System.out.println(" Loaded " + responses.size() + " messages for conversation " + conversationId);
            return responses;

        } catch (Exception e) {
            System.err.println(" Error loading chat history by conversation: " + e.getMessage());
            throw new RuntimeException("Không thể tải lịch sử chat", e);
        }
    }

    @Transactional
    public MessageReadResponse updateMessageStatus(Long messageId, boolean isRead) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tin nhắn"));

        message.setRead(isRead);
        message = messageRepository.save(message);

        // Lấy thông tin người đọc
        User reader = message.getReceiver();

        // Tạo response
        MessageReadResponse response = new MessageReadResponse();
        response.setMessageId(messageId);
        response.setReadBy(reader.getId());
        response.setReadByAvatar(reader.getAvatarUrl());
        response.setReadAt(LocalDateTime.now());
        response.setStatus("READ");

        return response;
    }

    @Transactional
    public void updateUserStatus(UserStatusDTO request) {
        try {
            System.out.println(" Updating status for userId: " + request.getUserId() + " to: " + request.getStatus());

            // Validate input
            if (request.getUserId() == null || request.getStatus() == null) {
                throw new IllegalArgumentException("UserId và Status không được null");
            }

            // Verify user exists
            if (!userRepository.existsById(request.getUserId())) {
                throw new IllegalArgumentException("User không tồn tại với ID: " + request.getUserId());
            }

            LocalDateTime now = LocalDateTime.now();

            // Tìm UserStatus hiện tại bằng userId
            UserStatus userStatus = userStatusRepository.findByUserId(request.getUserId()).orElse(null);

            if (userStatus == null) {
                // Tạo mới UserStatus
                System.out.println("Creating new UserStatus for user: " + request.getUserId());
                userStatus = new UserStatus();
                userStatus.setUserId(request.getUserId());
                userStatus.setCreatedAt(now);
                userStatus.setUpdatedAt(now);
                userStatus.setLastSeen(now);
            }

            // Cập nhật status
            if ("ONLINE".equals(request.getStatus())) {
                userStatus.setOnline(true);
                userStatus.setLastSeen(now);
                System.out.println("Setting user " + request.getUserId() + " to ONLINE");
            } else if ("OFFLINE".equals(request.getStatus())) {
                userStatus.setOnline(false);
                userStatus.setLastSeen(now);
                System.out.println(" Setting user " + request.getUserId() + " to OFFLINE");
            }

            userStatus.setUpdatedAt(now);

            // Save entity
            userStatus = userStatusRepository.save(userStatus);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Không thể cập nhật trạng thái người dùng: " + e.getMessage(), e);
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