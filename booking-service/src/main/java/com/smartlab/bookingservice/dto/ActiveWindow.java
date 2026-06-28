package com.smartlab.bookingservice.dto;

import java.time.LocalDateTime;

/**
 * A single active hold on a physical item: the per-item state plus the parent
 * booking's [start, end] window. Populated directly by a JPQL constructor query
 * in {@code BookingItemRepository.findActiveWindows}.
 */
public record ActiveWindow(
        Long itemId,
        String state,
        LocalDateTime start,
        LocalDateTime end) {
}
