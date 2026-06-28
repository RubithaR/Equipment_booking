package com.smartlab.bookingservice.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Booking-derived availability for one physical item, for the student catalogue
 * and cart. {@code status} is the headline bucket ({@code AVAILABLE},
 * {@code IN_PROCESS}, or {@code IN_USE}); {@code bookedUntil} is the latest
 * return date among active holds; {@code windows} lists each hold so the cart
 * can flag overlaps with a chosen date range.
 */
public record ItemAvailabilityResponse(
        Long itemId,
        String status,
        LocalDateTime bookedUntil,
        List<Window> windows) {

    public record Window(
            LocalDateTime start,
            LocalDateTime end,
            String state,
            String bucket) {
    }
}
