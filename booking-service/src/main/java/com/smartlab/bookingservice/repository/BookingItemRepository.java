package com.smartlab.bookingservice.repository;

import com.smartlab.bookingservice.entity.BookingItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingItemRepository extends JpaRepository<BookingItem, Long> {
    List<BookingItem> findByBookingIdOrderByIdAsc(Long bookingId);
    List<BookingItem> findByBookingIdInOrderByBookingIdAscIdAsc(java.util.Collection<Long> bookingIds);
    List<BookingItem> findByInstructorUserIdOrderByCreatedAtDesc(Long instructorUserId);
    List<BookingItem> findByAssignedHodUserIdOrderByCreatedAtDesc(Long hodUserId);

    /**
     * Time conflicts on a physical item — any booking_item still ACTIVE
     * (holds the item) whose [start_date, return_date] window from the parent
     * booking overlaps the requested window. Mirrors {@code BookingState.ACTIVE}.
     */
    @Query("""
            SELECT bi FROM BookingItem bi
            JOIN Booking b ON b.id = bi.bookingId
            WHERE bi.itemId = :itemId
              AND bi.state IN ('AWAITING_HOD','SUBMITTED','INSTRUCTOR_REVIEWING',
                               'READY_FOR_COLLECTION','LAB_CONFIRMED','COLLECTED','OVERDUE')
              AND b.startDate < :end
              AND b.returnDate > :start
            """)
    List<BookingItem> findConflicts(@Param("itemId") Long itemId,
                                    @Param("start") LocalDateTime start,
                                    @Param("end") LocalDateTime end);

    /** Items that should now be flipped to OVERDUE — past their booking's return date but still held. */
    @Query("""
            SELECT bi FROM BookingItem bi
            JOIN Booking b ON b.id = bi.bookingId
            WHERE bi.state = 'COLLECTED' AND b.returnDate < :cutoff
            """)
    List<BookingItem> findCollectedPastDue(@Param("cutoff") LocalDateTime cutoff);
}
