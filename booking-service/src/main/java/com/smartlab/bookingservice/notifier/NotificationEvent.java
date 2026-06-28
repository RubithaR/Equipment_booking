package com.smartlab.bookingservice.notifier;

import java.time.LocalDateTime;
import java.util.List;

public sealed interface NotificationEvent permits
        NotificationEvent.SubmittedDigestToInstructor,
        NotificationEvent.SubmittedAckToStudent,
        NotificationEvent.DelegatedToSupervisor,
        NotificationEvent.DelegatedAckToStudent,
        NotificationEvent.SupervisorApproved,
        NotificationEvent.SupervisorDeclinedToInstructor,
        NotificationEvent.SupervisorDeclinedToStudent,
        NotificationEvent.ReadyForCollection,
        NotificationEvent.ItemRejected,
        NotificationEvent.BookingCancelled,
        NotificationEvent.OverdueToStudent,
        NotificationEvent.OverdueToInstructor {

    /** One per instructor per submission, even if their lab has several lines. */
    record SubmittedDigestToInstructor(
            Long bookingId,
            Long instructorUserId,
            String studentFullName,
            String labName,
            String projectName,
            List<String> itemNames,
            LocalDateTime startDate,
            LocalDateTime returnDate) implements NotificationEvent {}

    record SubmittedAckToStudent(
            Long bookingId,
            Long studentUserId,
            int itemCount,
            int labCount) implements NotificationEvent {}

    record DelegatedToSupervisor(
            Long bookingId,
            Long bookingItemId,
            Long supervisorUserId,
            String studentFullName,
            String itemName,
            String labName,
            String projectName,
            LocalDateTime startDate,
            LocalDateTime returnDate,
            String note) implements NotificationEvent {}

    record DelegatedAckToStudent(
            Long bookingId,
            Long studentUserId,
            String itemName,
            String supervisorFullName) implements NotificationEvent {}

    record SupervisorApproved(
            Long bookingId,
            Long bookingItemId,
            Long instructorUserId,
            String studentFullName,
            String itemName,
            String supervisorFullName,
            String note) implements NotificationEvent {}

    record SupervisorDeclinedToInstructor(
            Long bookingId,
            Long bookingItemId,
            Long instructorUserId,
            String studentFullName,
            String itemName,
            String supervisorFullName,
            String reason) implements NotificationEvent {}

    record SupervisorDeclinedToStudent(
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
