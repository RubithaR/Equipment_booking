package com.smartlab.bookingservice.repository;

import com.smartlab.bookingservice.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /** Full thread in send order, oldest first — what the chat window renders. */
    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    /** Newest message in a thread, for the conversation-list preview. */
    ChatMessage findFirstByConversationIdOrderByCreatedAtDesc(Long conversationId);

    /** Unread count for a viewer: messages from the other party sent after they last read. */
    long countByConversationIdAndSenderUserIdNotAndCreatedAtAfter(
            Long conversationId, Long senderUserId, Instant after);
}
