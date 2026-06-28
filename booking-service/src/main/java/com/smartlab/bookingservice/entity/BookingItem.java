package com.smartlab.bookingservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * One physical item attached to an umbrella booking. Each item runs its own
 * approval lifecycle (one instructor per lab, optional supervisor delegation),
 * its own pickup details, and its own collected/returned/overdue tracking.
 */
@Entity
@Table(name = "booking_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "lab_id", nullable = false)
    private Long labId;

    /** The instructor assigned to this item's lab at the moment of submission. */
    @Column(name = "instructor_user_id", nullable = false)
    private Long instructorUserId;

    /** Set when the instructor delegates this line to a supervisor. */
    @Column(name = "assigned_supervisor_user_id")
    private Long assignedSupervisorUserId;

    @Column(nullable = false, length = 40)
    private String state;

    @Column(name = "pickup_at")
    private LocalDateTime pickupAt;

    @Column(name = "pickup_note", columnDefinition = "TEXT")
    private String pickupNote;

    @Column(name = "last_actor_user_id")
    private Long lastActorUserId;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false)
    private Instant updatedAt;
}
