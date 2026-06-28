package com.smartlab.bookingservice.repository;

import com.smartlab.bookingservice.entity.BookingEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingEventRepository extends JpaRepository<BookingEvent, Long> {
    List<BookingEvent> findByBookingIdOrderByCreatedAtAsc(Long bookingId);
}
