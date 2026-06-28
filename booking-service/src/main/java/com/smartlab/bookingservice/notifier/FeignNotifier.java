package com.smartlab.bookingservice.notifier;

import com.smartlab.notificationclient.NotificationClient;
import com.smartlab.notificationclient.NotificationDispatchRequest;
import com.smartlab.notificationclient.Notifier;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class FeignNotifier implements Notifier<NotificationEvent> {

    private static final Logger log = LoggerFactory.getLogger(FeignNotifier.class);

    private final NotificationClient notificationClient;

    @Override
    public void publish(NotificationEvent event) {
        switch (event) {
            case NotificationEvent.SubmittedDigestToInstructor e -> dispatch(
                    e.instructorUserId(), "BOOKING_NEEDS_REVIEW", Map.of(
                            "bookingId",       e.bookingId(),
                            "studentFullName", e.studentFullName(),
                            "labName",         e.labName(),
                            "projectName",     e.projectName(),
                            "itemNames",       e.itemNames(),
                            "startDate",       str(e.startDate()),
                            "returnDate",      str(e.returnDate())));

            case NotificationEvent.SubmittedAckToStudent e -> dispatch(
                    e.studentUserId(), "BOOKING_SUBMITTED", Map.of(
                            "bookingId", e.bookingId(),
                            "itemCount", e.itemCount(),
                            "labCount",  e.labCount()));

            case NotificationEvent.DelegatedToSupervisor e -> {
                Map<String, Object> p = new HashMap<>();
                p.put("bookingId",       e.bookingId());
                p.put("bookingItemId",   e.bookingItemId());
                p.put("studentFullName", e.studentFullName());
                p.put("itemName",        e.itemName());
                p.put("labName",         e.labName());
                p.put("projectName",     e.projectName());
                p.put("startDate",       str(e.startDate()));
                p.put("returnDate",      str(e.returnDate()));
                if (e.note() != null) p.put("note", e.note());
                dispatch(e.supervisorUserId(), "BOOKING_NEEDS_SUPERVISOR_APPROVAL", p);
            }

            case NotificationEvent.DelegatedAckToStudent e -> dispatch(
                    e.studentUserId(), "BOOKING_DELEGATED", Map.of(
                            "bookingId",           e.bookingId(),
                            "itemName",            e.itemName(),
                            "supervisorFullName",  e.supervisorFullName()));

            case NotificationEvent.SupervisorApproved e -> {
                Map<String, Object> p = new HashMap<>();
                p.put("bookingId",        e.bookingId());
                p.put("bookingItemId",    e.bookingItemId());
                p.put("studentFullName",  e.studentFullName());
                p.put("itemName",         e.itemName());
                p.put("supervisorFullName", e.supervisorFullName());
                if (e.note() != null) p.put("note", e.note());
                dispatch(e.instructorUserId(), "BOOKING_SUPERVISOR_APPROVED", p);
            }

            case NotificationEvent.SupervisorDeclinedToInstructor e -> {
                Map<String, Object> p = new HashMap<>();
                p.put("bookingId",          e.bookingId());
                p.put("bookingItemId",      e.bookingItemId());
                p.put("studentFullName",    e.studentFullName());
                p.put("itemName",           e.itemName());
                p.put("supervisorFullName", e.supervisorFullName());
                if (e.reason() != null) p.put("reason", e.reason());
                dispatch(e.instructorUserId(), "BOOKING_SUPERVISOR_DECLINED_INSTRUCTOR", p);
            }

            case NotificationEvent.SupervisorDeclinedToStudent e -> {
                Map<String, Object> p = new HashMap<>();
                p.put("bookingId", e.bookingId());
                p.put("itemName",  e.itemName());
                if (e.reason() != null) p.put("reason", e.reason());
                dispatch(e.studentUserId(), "BOOKING_SUPERVISOR_DECLINED_STUDENT", p);
            }

            case NotificationEvent.ReadyForCollection e -> {
                Map<String, Object> p = new HashMap<>();
                p.put("bookingId",      e.bookingId());
                p.put("itemName",       e.itemName());
                p.put("instructorName", e.instructorName());
                p.put("instructorEmail", e.instructorEmail());
                p.put("pickupAt",       str(e.pickupAt()));
                if (e.instructorPhone() != null) p.put("instructorPhone", e.instructorPhone());
                if (e.pickupNote()      != null) p.put("pickupNote",      e.pickupNote());
                dispatch(e.studentUserId(), "BOOKING_APPROVED", p);
            }

            case NotificationEvent.ItemRejected e -> {
                Map<String, Object> p = new HashMap<>();
                p.put("bookingId", e.bookingId());
                p.put("itemName",  e.itemName());
                if (e.reason() != null) p.put("reason", e.reason());
                dispatch(e.studentUserId(), "BOOKING_REJECTED", p);
            }

            case NotificationEvent.BookingCancelled e -> dispatch(
                    e.recipientUserId(), "BOOKING_CANCELLED", Map.of(
                            "bookingId",         e.bookingId(),
                            "affectedItemCount", e.affectedItemCount()));

            case NotificationEvent.OverdueToStudent e -> dispatch(
                    e.studentUserId(), "BOOKING_OVERDUE_STUDENT", Map.of(
                            "bookingId",  e.bookingId(),
                            "itemName",   e.itemName(),
                            "returnDate", str(e.returnDate())));

            case NotificationEvent.OverdueToInstructor e -> dispatch(
                    e.instructorUserId(), "BOOKING_OVERDUE_INSTRUCTOR", Map.of(
                            "bookingId",       e.bookingId(),
                            "studentFullName", e.studentFullName(),
                            "itemName",        e.itemName(),
                            "returnDate",      str(e.returnDate())));
        }
    }

    private void dispatch(Long userId, String eventType, Map<String, Object> payload) {
        if (userId == null) {
            log.warn("notifier.dispatch.skipped eventType={} reason=no-recipient", eventType);
            return;
        }
        try {
            notificationClient.send(new NotificationDispatchRequest(userId, eventType, payload));
        } catch (Exception ex) {
            log.error("notifier.dispatch.failed eventType={} recipientId={}", eventType, userId, ex);
        }
    }

    private static String str(Object v) {
        return v != null ? v.toString() : "";
    }
}
