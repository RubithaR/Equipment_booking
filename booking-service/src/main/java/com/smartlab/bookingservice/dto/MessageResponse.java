package com.smartlab.bookingservice.dto;

import com.smartlab.bookingservice.entity.ChatMessage;

import java.time.Instant;

/** One chat message. {@code mine} is true when the current viewer is the sender. */
public record MessageResponse(
        Long id,
        Long conversationId,
        Long senderUserId,
        String body,
        Instant createdAt,
        boolean mine) {

    public static MessageResponse from(ChatMessage m, Long viewerUserId) {
        return new MessageResponse(
                m.getId(), m.getConversationId(), m.getSenderUserId(),
                m.getBody(), m.getCreatedAt(), viewerUserId.equals(m.getSenderUserId()));
    }
}
