package com.smartlab.bookingservice.dto;

import java.time.Instant;

/**
 * One row in a user's chat list. {@code other*} fields describe the person on the
 * far side of the thread (so the student sees the instructor and vice versa).
 */
public record ConversationResponse(
        Long id,
        Long bookingId,
        String projectName,
        Long otherUserId,
        String otherUserName,
        String otherUserRole,
        String lastMessage,
        Instant lastMessageAt,
        long unreadCount,
        boolean iAmStudent) {}
