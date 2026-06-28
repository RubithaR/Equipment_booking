package com.smartlab.bookingservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "booking_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    /** Null when the event is at the umbrella level (e.g. SUBMITTED, CANCELLED). */
    @Column(name = "booking_item_id")
    private Long bookingItemId;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "from_state", length = 40)
    private String fromState;

    @Column(name = "to_state", nullable = false, length = 40)
    private String toState;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private Instant createdAt;
}
