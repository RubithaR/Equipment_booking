package com.smartlab.bookingservice.notifier;

import java.time.LocalDateTime;

/**
 * Booking notifications, modelled after the HoD-first flow:
 * submit -> HoD approves/rejects -> instructor approves (ready for collection,
 * or, for lab-only items, confirms an available time) / rejects.
 */
public sealed interface NotificationEvent permits
        NotificationEvent.SubmittedAckToStudent,
        NotificationEvent.HodReviewNeeded,
        NotificationEvent.HodApprovedToInstructor,
        NotificationEvent.HodDeclinedToStudent,
        NotificationEvent.ReadyForCollection,
        NotificationEvent.LabConfirmed,
        NotificationEvent.ItemRejected,
        NotificationEvent.BookingCancelled,
        NotificationEvent.OverdueToStudent,
        NotificationEvent.OverdueToInstructor {

    record SubmittedAckToStudent(
            Long bookingId,
            Long studentUserId,
            int itemCount,
            int labCount) implements NotificationEvent {}

    /** Stage 1: a line landed in the department HoD's queue for approval. */
    record HodReviewNeeded(
            Long bookingId,
            Long bookingItemId,
            Long hodUserId,
            String studentFullName,
            String itemName,
            String usageType,
            String labName,
            String projectName,
            LocalDateTime startDate,
            LocalDateTime returnDate,
            LocalDateTime requestedUseTime) implements NotificationEvent {}

    /** Stage 2: the HoD approved; the line now needs the lab instructor's review. */
    record HodApprovedToInstructor(
            Long bookingId,
            Long bookingItemId,
            Long instructorUserId,
            String studentFullName,
            String itemName,
            String usageType,
            String labName,
            String projectName,
            LocalDateTime startDate,
            LocalDateTime returnDate,
            LocalDateTime requestedUseTime,
            String note) implements NotificationEvent {}

    record HodDeclinedToStudent(
            Long bookingId,
            Long bookingItemId,
            Long studentUserId,
            String itemName,
            String reason) implements NotificationEvent {}

    record ReadyForCollection(
            Long bookingId,
            Long bookingItemId,
            Long studentUserId,
            String itemName,
            String instructorName,
            String instructorEmail,
            String instructorPhone,
            LocalDateTime pickupAt,
            String pickupNote) implements NotificationEvent {}

    /** Lab-only: the instructor confirmed a time for the student to use the item in the lab. */
    record LabConfirmed(
            Long bookingId,
            Long bookingItemId,
            Long studentUserId,
            String itemName,
            String labName,
            String instructorName,
            String instructorEmail,
            String instructorPhone,
            LocalDateTime availableTime,
            String note) implements NotificationEvent {}

    record ItemRejected(
            Long bookingId,
            Long bookingItemId,
            Long studentUserId,
            String itemName,
            String reason) implements NotificationEvent {}

    record BookingCancelled(
            Long bookingId,
            Long recipientUserId,
            int affectedItemCount) implements NotificationEvent {}

    record OverdueToStudent(
            Long bookingId,
            Long bookingItemId,
            Long studentUserId,
            String itemName,
            LocalDateTime returnDate) implements NotificationEvent {}

    record OverdueToInstructor(
            Long bookingId,
            Long bookingItemId,
            Long instructorUserId,
            String studentFullName,
            String itemName,
            LocalDateTime returnDate) implements NotificationEvent {}
}
