package com.smartlab.notificationservice.service;

import com.smartlab.notificationclient.NotificationDispatchRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Owns all human-readable notification copy. Callers dispatch a typed event
 * with a structured payload; this class resolves it to a {title, message} pair
 * before the record is persisted. Adding or changing wording never touches a
 * calling service.
 */
@Component
public class MessageRenderer {

    public record Rendered(String title, String message) {}

    public Rendered render(NotificationDispatchRequest req) {
        Map<String, Object> p = req.getPayload() != null ? req.getPayload() : Map.of();
        return switch (req.getEventType()) {

            case "BOOKING_NEEDS_REVIEW" -> new Rendered(
                    "New booking awaiting your review",
                    str(p, "studentFullName") + " requested "
                            + summariseItems(list(p, "itemNames"))
                            + " from " + str(p, "labName")
                            + " · " + str(p, "projectName")
                            + " · " + str(p, "startDate")
                            + " → " + str(p, "returnDate"));

            case "BOOKING_SUBMITTED" -> new Rendered(
                    "Booking submitted",
                    "Your booking #" + str(p, "bookingId")
                            + " (" + num(p, "itemCount") + " item" + (num(p, "itemCount") == 1 ? "" : "s")
                            + " across " + num(p, "labCount") + " lab" + (num(p, "labCount") == 1 ? "" : "s")
                            + ") is awaiting instructor review.");

            case "BOOKING_NEEDS_SUPERVISOR_APPROVAL" -> new Rendered(
                    "Booking awaiting your approval",
                    "Instructor forwarded line #" + str(p, "bookingItemId")
                            + " of booking #" + str(p, "bookingId")
                            + " for your sign-off: " + str(p, "studentFullName")
                            + " requested " + str(p, "itemName")
                            + " from " + str(p, "labName")
                            + " · " + str(p, "projectName")
                            + " · " + str(p, "startDate") + " → " + str(p, "returnDate")
                            + noteClause(strOrNull(p, "note"), "Instructor's note"));

            case "BOOKING_DELEGATED" -> new Rendered(
                    "Booking line forwarded to supervisor",
                    "Your booking #" + str(p, "bookingId")
                            + " line for " + str(p, "itemName")
                            + " has been forwarded to " + str(p, "supervisorFullName")
                            + " for approval.");

            case "BOOKING_SUPERVISOR_APPROVED" -> new Rendered(
                    "Supervisor approved a booking line",
                    str(p, "supervisorFullName") + " approved line #" + str(p, "bookingItemId")
                            + " of booking #" + str(p, "bookingId")
                            + " (" + str(p, "studentFullName") + " · " + str(p, "itemName")
                            + "). Set the pickup details to release the item."
                            + noteClause(strOrNull(p, "note"), "Supervisor's note"));

            case "BOOKING_SUPERVISOR_DECLINED_INSTRUCTOR" -> new Rendered(
                    "Supervisor declined a booking line",
                    str(p, "supervisorFullName") + " declined line #" + str(p, "bookingItemId")
                            + " of booking #" + str(p, "bookingId")
                            + " (" + str(p, "studentFullName") + " · " + str(p, "itemName") + ")."
                            + noteClause(strOrNull(p, "reason"), "Reason"));

            case "BOOKING_SUPERVISOR_DECLINED_STUDENT" -> new Rendered(
                    "Booking line declined by supervisor",
                    "Your booking #" + str(p, "bookingId")
                            + " line for " + str(p, "itemName")
                            + " was declined by the supervisor."
                            + noteClause(strOrNull(p, "reason"), "Reason"));

            case "BOOKING_APPROVED" -> new Rendered(
                    "Booking line approved — ready for collection",
                    "Your booking #" + str(p, "bookingId")
                            + " line for " + str(p, "itemName")
                            + " is approved. Collect it at " + str(p, "pickupAt")
                            + " from " + str(p, "instructorName")
                            + " (" + str(p, "instructorEmail")
                            + (strOrNull(p, "instructorPhone") != null ? ", " + str(p, "instructorPhone") : "") + ")."
                            + noteClause(strOrNull(p, "pickupNote"), "Note"));

            case "BOOKING_REJECTED" -> new Rendered(
                    "Booking line rejected",
                    "Your booking #" + str(p, "bookingId")
                            + " line for " + str(p, "itemName") + " was rejected."
                            + noteClause(strOrNull(p, "reason"), "Reason"));

            case "BOOKING_CANCELLED" -> new Rendered(
                    "Booking cancelled",
                    "Booking #" + str(p, "bookingId")
                            + " has been cancelled by the student"
                            + (num(p, "affectedItemCount") > 0
                                    ? " (" + num(p, "affectedItemCount") + " line"
                                      + (num(p, "affectedItemCount") == 1 ? "" : "s") + " released)" : "") + ".");

            case "BOOKING_OVERDUE_STUDENT" -> new Rendered(
                    "Item overdue — please return",
                    "Your booking #" + str(p, "bookingId")
                            + " line for " + str(p, "itemName")
                            + " was due back at " + str(p, "returnDate")
                            + ". Please return it as soon as possible.");

            case "BOOKING_OVERDUE_INSTRUCTOR" -> new Rendered(
                    "Booking line now overdue",
                    "Booking #" + str(p, "bookingId")
                            + " — " + str(p, "studentFullName")
                            + " has not returned " + str(p, "itemName")
                            + " (due " + str(p, "returnDate") + ").");

            case "INSTRUCTOR_ACCOUNT_APPROVED" -> new Rendered(
                    "Instructor account approved",
                    "Your account has been approved by admin. You can now log in.");

            default -> new Rendered(
                    "Notification",
                    "You have a new notification (type: " + req.getEventType() + ").");
        };
    }

    // ===== helpers =====

    private static String str(Map<String, Object> p, String key) {
        Object v = p.get(key);
        return v != null ? v.toString() : "";
    }

    private static String strOrNull(Map<String, Object> p, String key) {
        Object v = p.get(key);
        return v != null ? v.toString() : null;
    }

    private static int num(Map<String, Object> p, String key) {
        Object v = p.get(key);
        if (v instanceof Number n) return n.intValue();
        return 0;
    }

    @SuppressWarnings("unchecked")
    private static List<String> list(Map<String, Object> p, String key) {
        Object v = p.get(key);
        if (v instanceof List<?> l) return (List<String>) l;
        return List.of();
    }

    private static String noteClause(String note, String label) {
        return (note != null && !note.isBlank()) ? " " + label + ": " + note : "";
    }

    private static String summariseItems(List<String> names) {
        if (names == null || names.isEmpty()) return "an item";
        if (names.size() == 1) return names.get(0);
        if (names.size() <= 3) return String.join(", ", names);
        return names.get(0) + " and " + (names.size() - 1) + " other item" + (names.size() - 1 == 1 ? "" : "s");
    }
}
