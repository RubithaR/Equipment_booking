package com.smartlab.bookingservice.dto;

import com.smartlab.bookingservice.entity.ChatMessage;

import java.time.Instant;

/**
 * One chat message. {@code mine} is true when the current viewer is the sender
 * (and therefore allowed to edit or delete it); {@code editedAt} is null until edited.
 */
public record MessageResponse(
        Long id,
        Long conversationId,
        Long senderUserId,
        String body,
        Instant createdAt,
        Instant editedAt,
        boolean mine) {

    public static MessageResponse from(ChatMessage m, Long viewerUserId) {
        return new MessageResponse(
                m.getId(), m.getConversationId(), m.getSenderUserId(),
                m.getBody(), m.getCreatedAt(), m.getEditedAt(),
                viewerUserId.equals(m.getSenderUserId()));
    }
}
