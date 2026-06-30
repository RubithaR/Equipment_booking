package com.smartlab.bookingservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * A two-party chat thread between the student who owns a booking and the
 * instructor who was assigned to handle it. Opened automatically when the
 * instructor approves a line (READY_FOR_COLLECTION). Unique per
 * (booking, instructor) so one instructor's chat about one booking is one thread.
 */
@Entity
@Table(name = "chat_conversations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "student_user_id", nullable = false)
    private Long studentUserId;

    @Column(name = "instructor_user_id", nullable = false)
    private Long instructorUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Timestamp of the most recent message — drives "newest first" ordering of the thread list. */
    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    /** How far the student has read; messages after this from the instructor count as unread. */
    @Column(name = "student_last_read_at")
    private Instant studentLastReadAt;

    /** How far the instructor has read; messages after this from the student count as unread. */
    @Column(name = "instructor_last_read_at")
    private Instant instructorLastReadAt;
}
