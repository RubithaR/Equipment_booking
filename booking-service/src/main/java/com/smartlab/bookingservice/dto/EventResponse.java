package com.smartlab.bookingservice.dto;

import com.smartlab.bookingservice.entity.BookingEvent;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class EventResponse {
    private Long id;
    private Long bookingId;
    private Long actorUserId;
    private String fromState;
    private String toState;
    private String note;
    private Instant createdAt;

    public static EventResponse from(BookingEvent e) {
        return new EventResponse(e.getId(), e.getBookingId(), e.getActorUserId(),
                e.getFromState(), e.getToState(), e.getNote(), e.getCreatedAt());
    }
}
