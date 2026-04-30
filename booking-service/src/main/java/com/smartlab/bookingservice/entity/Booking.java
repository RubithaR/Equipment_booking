package com.smartlab.bookingservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
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

    @Column(name = "student_user_id", nullable = false)
    private Long studentUserId;

    @Column(name = "student_department_id", nullable = false)
    private Long studentDepartmentId;

    @Column(name = "project_name", nullable = false, length = 300)
    private String projectName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String purpose;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "return_date", nullable = false)
    private LocalDateTime returnDate;

    /** Optional supervisor the student suggested up front; instructors use it as the default delegate target. */
    @Column(name = "nominated_supervisor_user_id")
    private Long nominatedSupervisorUserId;

    /**
     * Aggregate state derived from the items. Recomputed on every per-item transition.
     * Possible values come from {@link BookingState}.
     */
    @Column(nullable = false, length = 40)
    private String state;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false)
    private Instant updatedAt;
}
