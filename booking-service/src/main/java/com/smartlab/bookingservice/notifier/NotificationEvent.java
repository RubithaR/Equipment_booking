package com.smartlab.bookingservice.notifier;

import java.time.LocalDateTime;

public sealed interface NotificationEvent permits
        NotificationEvent.BookingSubmitted,
        NotificationEvent.BookingApproved,
        NotificationEvent.BookingRejected,
        NotificationEvent.BookingCancelled {

    record BookingSubmitted(
            Long bookingId,
            Long studentId,
            String studentFullName,
            String equipmentName,
            LocalDateTime startTime,
            LocalDateTime endTime) implements NotificationEvent {}

    record BookingApproved(Long bookingId, Long studentId) implements NotificationEvent {}

    record BookingRejected(Long bookingId, Long studentId, String reason) implements NotificationEvent {}

    record BookingCancelled(Long bookingId, Long studentId) implements NotificationEvent {}
}
