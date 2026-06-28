package com.smartlab.bookingservice.repository;

import com.smartlab.bookingservice.dto.ActiveWindow;
import com.smartlab.bookingservice.entity.BookingItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface BookingItemRepository extends JpaRepository<BookingItem, Long> {
    List<BookingItem> findByBookingIdOrderByIdAsc(Long bookingId);
    List<BookingItem> findByBookingIdInOrderByBookingIdAscIdAsc(java.util.Collection<Long> bookingIds);
    List<BookingItem> findByInstructorUserIdOrderByCreatedAtDesc(Long instructorUserId);
    List<BookingItem> findByAssignedSupervisorUserIdOrderByCreatedAtDesc(Long supervisorUserId);

    /**
     * Time conflicts on a physical item — any booking_item still ACTIVE
     * (holds the item) whose [start_date, return_date] window from the parent
     * booking overlaps the requested window.
     */
    @Query("""
            SELECT bi FROM BookingItem bi
            JOIN Booking b ON b.id = bi.bookingId
            WHERE bi.itemId = :itemId
              AND bi.state IN ('SUBMITTED','INSTRUCTOR_REVIEWING','AWAITING_SUPERVISOR',
                               'SUPERVISOR_APPROVED','READY_FOR_COLLECTION','COLLECTED','OVERDUE')
              AND b.startDate < :end
              AND b.returnDate > :start
            """)
    List<BookingItem> findConflicts(@Param("itemId") Long itemId,
                                    @Param("start") LocalDateTime start,
                                    @Param("end") LocalDateTime end);

    /**
     * Active holds (state-wise) across a set of items, each paired with its parent
     * booking's window. Drives the student-facing availability view — "in process"
     * vs "in use" plus the date each item is booked until.
     */
    @Query("""
            SELECT new com.smartlab.bookingservice.dto.ActiveWindow(
                       bi.itemId, bi.state, b.startDate, b.returnDate)
            FROM BookingItem bi
            JOIN Booking b ON b.id = bi.bookingId
            WHERE bi.itemId IN :itemIds
              AND bi.state IN ('SUBMITTED','INSTRUCTOR_REVIEWING','AWAITING_SUPERVISOR',
                               'SUPERVISOR_APPROVED','READY_FOR_COLLECTION','COLLECTED','OVERDUE')
            ORDER BY b.startDate ASC
            """)
    List<ActiveWindow> findActiveWindows(@Param("itemIds") Collection<Long> itemIds);

    /** Items that should now be flipped to OVERDUE — past their booking's return date but still held. */
    @Query("""
            SELECT bi FROM BookingItem bi
            JOIN Booking b ON b.id = bi.bookingId
            WHERE bi.state = 'COLLECTED' AND b.returnDate < :cutoff
            """)
    List<BookingItem> findCollectedPastDue(@Param("cutoff") LocalDateTime cutoff);
}
