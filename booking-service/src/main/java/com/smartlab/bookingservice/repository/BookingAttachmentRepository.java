package com.smartlab.bookingservice.repository;

import com.smartlab.bookingservice.entity.BookingAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingAttachmentRepository extends JpaRepository<BookingAttachment, Long> {
    List<BookingAttachment> findByBookingIdOrderByCreatedAtAsc(Long bookingId);
}
