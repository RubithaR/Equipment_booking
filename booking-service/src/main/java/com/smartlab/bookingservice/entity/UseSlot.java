package com.smartlab.bookingservice.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * One proposed lab-use time slot on a LAB_ONLY booking line. The student proposes a
 * set of these (each a {@code from}-{@code to} window across their booking window);
 * the assigned handler ticks the ones they're available for, flipping {@code confirmed}
 * to true. {@code at} holds the slot start ("from"); {@code to} holds the slot end.
 * Persisted as JSON in {@code booking_items.use_slots} via {@link UseSlotListConverter}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UseSlot {
    /** Slot start ("from"). Slots are matched/confirmed by this value. */
    private LocalDateTime at;
    /** Slot end ("to"). Null on legacy single-instant slots. */
    private LocalDateTime to;
    private boolean confirmed;

    /** Legacy single-instant slot (no end time). */
    public UseSlot(LocalDateTime at, boolean confirmed) {
        this(at, null, confirmed);
    }
}
