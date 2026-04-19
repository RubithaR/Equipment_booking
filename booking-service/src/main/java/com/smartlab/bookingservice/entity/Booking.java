package com.smartlab.bookingservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long equipmentId;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    // PENDING_APPROVAL, CONFIRMED, REJECTED, CANCELLED
    @Column(nullable = false)
    private String status;

    private String purpose;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // Set when an instructor approves/rejects
    private Long reviewedByInstructorId;
    private LocalDateTime reviewedAt;
    private String reviewNote;
}
