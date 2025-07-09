package com.hospital.telemedicine.repository;

import com.hospital.telemedicine.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("SELECT m FROM Message m WHERE (m.sender.id = :userId1 AND m.receiver.id = :userId2) " +
            "OR (m.sender.id = :userId2 AND m.receiver.id = :userId1) ORDER BY m.sentAt ASC")
    List<Message> findChatHistory(Long userId1, Long userId2);

    List<Message> findByConversationId(Long conversationId);

    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId ORDER BY m.sentAt ASC")
    List<Message> findByConversationIdOrderBySentAt(@Param("conversationId") Long conversationId);

    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId " +
            "AND m.receiver.id = :receiverId AND m.isRead = false")
    List<Message> findUnreadMessagesByConversationAndReceiver(
            @Param("conversationId") Long conversationId,
            @Param("receiverId") Long receiverId);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.receiver.id = :userId AND m.isRead = false")
    Long countUnreadMessages(@Param("userId") Long userId);

    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId " +
            "ORDER BY m.sentAt DESC LIMIT 1")
    Optional<Message> findLatestMessageByConversationId(@Param("conversationId") Long conversationId);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversation.id = :conversationId " +
            "AND m.receiver.id = :userId AND m.isRead = false")
    Long countUnreadMessagesByConversationAndUser(@Param("conversationId") Long conversationId,
                                                  @Param("userId") Long userId);


}