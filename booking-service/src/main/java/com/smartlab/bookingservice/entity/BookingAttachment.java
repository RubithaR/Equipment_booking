package com.smartlab.bookingservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "booking_attachments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "file_url", nullable = false, length = 1000)
    private String fileUrl;

    @Column(name = "file_name", nullable = false, length = 300)
    private String fileName;

    // REQUEST_LETTER, SUPERVISOR_LETTER, OTHER
    @Column(nullable = false, length = 50)
    private String kind;

    @Column(name = "uploaded_by_user_id")
    private Long uploadedByUserId;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private Instant createdAt;
}
