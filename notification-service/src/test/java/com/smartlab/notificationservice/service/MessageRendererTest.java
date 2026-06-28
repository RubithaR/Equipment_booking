package com.smartlab.notificationservice.service;

import com.smartlab.notificationclient.NotificationDispatchRequest;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pinning the wording of every notification template. The render path is the
 * canonical home for notification copy — this is the test surface for that.
 */
class MessageRendererTest {

    MessageRenderer renderer = new MessageRenderer();

    private NotificationDispatchRequest req(String eventType, Map<String, Object> payload) {
        return new NotificationDispatchRequest(1L, eventType, payload);
    }

    @Test
    void bookingNeedsReview_listsItemsAndProject() {
        var r = renderer.render(req("BOOKING_NEEDS_REVIEW", Map.of(
                "studentFullName", "Alice",
                "labName",         "Electronics Lab",
                "projectName",     "FYP",
                "itemNames",       List.of("Oscilloscope"),
                "startDate",       "2026-05-01T09:00",
                "returnDate",      "2026-05-02T17:00")));

        assertThat(r.title()).isEqualTo("New booking awaiting your review");
        assertThat(r.message())
                .contains("Alice", "Oscilloscope", "Electronics Lab", "FYP", "2026-05-01T09:00", "2026-05-02T17:00");
    }

    @Test
    void bookingSubmitted_singularItemAndLab() {
        var r = renderer.render(req("BOOKING_SUBMITTED", Map.of(
                "bookingId", 1L, "itemCount", 1, "labCount", 1)));
        assertThat(r.title()).isEqualTo("Booking submitted");
        assertThat(r.message()).contains("1 item across 1 lab", "#1");
    }

    @Test
    void bookingSubmitted_pluralItemsAndLabs() {
        var r = renderer.render(req("BOOKING_SUBMITTED", Map.of(
                "bookingId", 1L, "itemCount", 3, "labCount", 2)));
        assertThat(r.message()).contains("3 items across 2 labs");
    }

    @Test
    void bookingNeedsSupervisorApproval_includesNoteWhenPresent() {
        Map<String, Object> p = new HashMap<>(Map.of(
                "bookingId",       1L,
                "bookingItemId",   10L,
                "studentFullName", "Alice",
                "itemName",        "DAQ",
                "labName",         "EE Lab",
                "projectName",     "FYP",
                "startDate",       "2026-05-01",
                "returnDate",      "2026-05-02"));
        p.put("note", "Specialist gear");

        var r = renderer.render(req("BOOKING_NEEDS_SUPERVISOR_APPROVAL", p));
        assertThat(r.title()).isEqualTo("Booking awaiting your approval");
        assertThat(r.message()).contains("Instructor's note: Specialist gear");
    }

    @Test
    void bookingNeedsSupervisorApproval_omitsNoteWhenAbsent() {
        var r = renderer.render(req("BOOKING_NEEDS_SUPERVISOR_APPROVAL", Map.of(
                "bookingId", 1L, "bookingItemId", 10L,
                "studentFullName", "Alice", "itemName", "DAQ", "labName", "EE Lab",
                "projectName", "FYP", "startDate", "2026-05-01", "returnDate", "2026-05-02")));
        assertThat(r.message()).doesNotContain("Instructor's note");
    }

    @Test
    void bookingDelegated_namesSupervisor() {
        var r = renderer.render(req("BOOKING_DELEGATED", Map.of(
                "bookingId", 1L, "itemName", "DAQ", "supervisorFullName", "Carol")));
        assertThat(r.title()).isEqualTo("Booking line forwarded to supervisor");
        assertThat(r.message()).contains("Carol");
    }

    @Test
    void bookingSupervisorApproved_includesNote() {
        Map<String, Object> p = new HashMap<>(Map.of(
                "bookingId", 1L, "bookingItemId", 10L,
                "studentFullName", "Alice", "itemName", "DAQ",
                "supervisorFullName", "Carol"));
        p.put("note", "OK");
        var r = renderer.render(req("BOOKING_SUPERVISOR_APPROVED", p));
        assertThat(r.message()).contains("Supervisor's note: OK");
    }

    @Test
    void bookingSupervisorDeclinedInstructor_includesReason() {
        Map<String, Object> p = new HashMap<>(Map.of(
                "bookingId", 1L, "bookingItemId", 10L,
                "studentFullName", "Alice", "itemName", "DAQ",
                "supervisorFullName", "Carol"));
        p.put("reason", "Already booked");
        var r = renderer.render(req("BOOKING_SUPERVISOR_DECLINED_INSTRUCTOR", p));
        assertThat(r.title()).isEqualTo("Supervisor declined a booking line");
        assertThat(r.message()).contains("Reason: Already booked");
    }

    @Test
    void bookingSupervisorDeclinedStudent_includesReason() {
        Map<String, Object> p = new HashMap<>(Map.of(
                "bookingId", 1L, "itemName", "DAQ"));
        Map<String, Object> p2 = new HashMap<>(p); p2.put("reason", "Conflict");
        var r = renderer.render(req("BOOKING_SUPERVISOR_DECLINED_STUDENT", p2));
        assertThat(r.message()).contains("Reason: Conflict");
    }

    @Test
    void bookingApproved_includesPickupAndContacts() {
        Map<String, Object> p = new HashMap<>(Map.of(
                "bookingId", 1L,
                "itemName", "Oscilloscope",
                "instructorName", "Bob",
                "instructorEmail", "bob@uni.com",
                "pickupAt", "2026-05-01T10:00"));
        p.put("instructorPhone", "0771234567");
        p.put("pickupNote", "ID required");
        var r = renderer.render(req("BOOKING_APPROVED", p));
        assertThat(r.title()).isEqualTo("Booking line approved — ready for collection");
        assertThat(r.message()).contains("Bob", "bob@uni.com", "0771234567", "Note: ID required");
    }

    @Test
    void bookingApproved_omitsOptionalFields() {
        var r = renderer.render(req("BOOKING_APPROVED", Map.of(
                "bookingId", 1L,
                "itemName", "Oscilloscope",
                "instructorName", "Bob",
                "instructorEmail", "bob@uni.com",
                "pickupAt", "2026-05-01T10:00")));
        assertThat(r.message()).doesNotContain("Note:");
    }

    @Test
    void bookingRejected_includesReason() {
        Map<String, Object> p = new HashMap<>(Map.of("bookingId", 1L, "itemName", "DAQ"));
        p.put("reason", "Wrong lab");
        var r = renderer.render(req("BOOKING_REJECTED", p));
        assertThat(r.title()).isEqualTo("Booking line rejected");
        assertThat(r.message()).contains("Reason: Wrong lab");
    }

    @Test
    void bookingCancelled_pluralisesAffectedItems() {
        var oneLine = renderer.render(req("BOOKING_CANCELLED", Map.of(
                "bookingId", 1L, "affectedItemCount", 1)));
        assertThat(oneLine.message()).contains("1 line released");

        var multiple = renderer.render(req("BOOKING_CANCELLED", Map.of(
                "bookingId", 1L, "affectedItemCount", 3)));
        assertThat(multiple.message()).contains("3 lines released");

        var zero = renderer.render(req("BOOKING_CANCELLED", Map.of(
                "bookingId", 1L, "affectedItemCount", 0)));
        assertThat(zero.message()).doesNotContain("released");
    }

    @Test
    void bookingOverdueStudent_namesItemAndDate() {
        var r = renderer.render(req("BOOKING_OVERDUE_STUDENT", Map.of(
                "bookingId", 1L, "itemName", "DAQ", "returnDate", "2026-05-01")));
        assertThat(r.title()).isEqualTo("Item overdue — please return");
        assertThat(r.message()).contains("DAQ", "2026-05-01");
    }

    @Test
    void bookingOverdueInstructor_namesStudentAndItem() {
        var r = renderer.render(req("BOOKING_OVERDUE_INSTRUCTOR", Map.of(
                "bookingId", 1L, "studentFullName", "Alice",
                "itemName", "DAQ", "returnDate", "2026-05-01")));
        assertThat(r.title()).isEqualTo("Booking line now overdue");
        assertThat(r.message()).contains("Alice", "DAQ", "2026-05-01");
    }

    @Test
    void instructorAccountApproved_fixedCopy() {
        var r = renderer.render(req("INSTRUCTOR_ACCOUNT_APPROVED", Map.of()));
        assertThat(r.title()).isEqualTo("Instructor account approved");
        assertThat(r.message()).isEqualTo("Your account has been approved by admin. You can now log in.");
    }

    @Test
    void unknownEventType_returnsFallbackInsteadOfThrowing() {
        var r = renderer.render(req("FUTURE_EVENT_TYPE", Map.of()));
        assertThat(r.title()).isEqualTo("Notification");
        assertThat(r.message()).contains("FUTURE_EVENT_TYPE");
    }

    @Test
    void itemNamesWithMoreThanThree_summarisesWithCount() {
        var r = renderer.render(req("BOOKING_NEEDS_REVIEW", Map.of(
                "studentFullName", "Alice",
                "labName",         "Lab",
                "projectName",     "FYP",
                "itemNames",       List.of("A", "B", "C", "D"),
                "startDate",       "2026-05-01",
                "returnDate",      "2026-05-02")));
        assertThat(r.message()).contains("A and 3 other items");
    }

    @Test
    void missingPayloadKey_silentlyResolvesToEmptyString_documentingCurrentBehavior() {
        // The renderer is tolerant of missing keys — required fields are documented
        // by the FeignNotifier dispatch sites; null callers fall through to "".
        var r = renderer.render(req("BOOKING_REJECTED", Map.of("bookingId", 1L)));
        assertThat(r.title()).isEqualTo("Booking line rejected");
        assertThat(r.message()).contains("#1");
    }
}
