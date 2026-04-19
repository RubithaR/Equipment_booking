package com.smartlab.bookingservice.repository;

import com.smartlab.bookingservice.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserId(Long userId);
    List<Booking> findByEquipmentId(Long equipmentId);
    List<Booking> findByStatus(String status);
    List<Booking> findByStatusOrderByCreatedAtDesc(String status);

    // Conflict = any booking on this equipment that is not REJECTED/CANCELLED and overlaps the time window.
    // Both PENDING_APPROVAL and CONFIRMED bookings reserve the slot.
    @Query("SELECT b FROM Booking b WHERE b.equipmentId = :equipmentId " +
           "AND b.status IN ('PENDING_APPROVAL', 'CONFIRMED') " +
           "AND b.startTime < :endTime AND b.endTime > :startTime")
    List<Booking> findConflicts(@Param("equipmentId") Long equipmentId,
                                @Param("startTime") LocalDateTime startTime,
                                @Param("endTime") LocalDateTime endTime);
}
