package com.smartlab.bookingservice.dto;

import com.smartlab.bookingservice.entity.BookingItem;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class BookingItemResponse {
    private Long id;
    private Long bookingId;
    private Long itemId;
    private Long labId;
    private Long instructorUserId;
    private Long assignedHodUserId;
    private Long assignedSupervisorUserId;
    private String usageType;
    private LocalDateTime requestedUseTime;
    private String state;
    private LocalDateTime pickupAt;
    private String pickupNote;
    private Long lastActorUserId;

    public static BookingItemResponse from(BookingItem bi) {
        return new BookingItemResponse(
                bi.getId(), bi.getBookingId(), bi.getItemId(), bi.getLabId(),
                bi.getInstructorUserId(), bi.getAssignedHodUserId(), bi.getAssignedSupervisorUserId(),
                bi.getUsageType(), bi.getRequestedUseTime(),
                bi.getState(), bi.getPickupAt(), bi.getPickupNote(),
                bi.getLastActorUserId());
    }
}
