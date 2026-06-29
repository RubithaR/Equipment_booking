package com.smartlab.bookingservice.transition;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.smartlab.bookingservice.entity.BookingItem;
import com.smartlab.bookingservice.entity.BookingState;
import com.smartlab.security.ItemStatus;
import com.smartlab.bookingservice.notifier.NotificationEvent;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The whole booking-item state machine in one closed set of values. Each record
 * declares the rule for its transition: from-states, to-state, who's allowed,
 * what to mutate, whether to flip the underlying item, and which notifications
 * to fire. The engine runs the same skeleton for every one of them.
 *
 * Flow: AWAITING_HOD --(HoD)--> SUBMITTED --(instructor)--> READY_FOR_COLLECTION
 * (borrowable) or LAB_CONFIRMED (lab-only). Either approver can reject.
 *
 * Wire shape: {@code { "type": "APPROVE_DIRECTLY", "pickupAt": "...", "pickupNote": "..." }}.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Transition.HodApprove.class,         name = "HOD_APPROVE"),
        @JsonSubTypes.Type(value = Transition.HodReject.class,          name = "HOD_REJECT"),
        @JsonSubTypes.Type(value = Transition.StartReview.class,        name = "START_REVIEW"),
        @JsonSubTypes.Type(value = Transition.ApproveDirectly.class,    name = "APPROVE_DIRECTLY"),
        @JsonSubTypes.Type(value = Transition.ConfirmLab.class,         name = "CONFIRM_LAB"),
        @JsonSubTypes.Type(value = Transition.Reject.class,             name = "REJECT"),
        @JsonSubTypes.Type(value = Transition.MarkCollected.class,      name = "MARK_COLLECTED"),
        @JsonSubTypes.Type(value = Transition.MarkReturned.class,       name = "MARK_RETURNED"),
        @JsonSubTypes.Type(value = Transition.FlipOverdue.class,        name = "FLIP_OVERDUE"),
        @JsonSubTypes.Type(value = Transition.Cancel.class,             name = "CANCEL"),
})
public sealed interface Transition permits
        Transition.HodApprove,
        Transition.HodReject,
        Transition.StartReview,
        Transition.ApproveDirectly,
        Transition.ConfirmLab,
        Transition.Reject,
        Transition.MarkCollected,
        Transition.MarkReturned,
        Transition.FlipOverdue,
        Transition.Cancel {

    Set<String> fromStates();
    String toState();
    Role requiredRole();

    default void mutate(BookingItem line) {}

    /** Optional item-status flip on the underlying physical unit (AVAILABLE/IN_USE). */
    default Optional<String> itemStatusFlip(String fromState) { return Optional.empty(); }

    /** False when this transition never produces notifications, so the engine skips remote context fetches. */
    default boolean hasNotifications() { return true; }

    default List<NotificationEvent> notifications(TransitionContext c) { return List.of(); }

    // ===== Records =====

    /** Stage 1: the department HoD approves; the line moves on to the lab instructor. */
    record HodApprove(String note) implements Transition {
        public Set<String> fromStates() { return Set.of(BookingState.AWAITING_HOD); }
        public String toState()         { return BookingState.SUBMITTED; }
        public Role requiredRole()      { return Role.HOD_ASSIGNED; }
        public List<NotificationEvent> notifications(TransitionContext c) {
            return List.of(new NotificationEvent.HodApprovedToInstructor(
                    c.booking().getId(), c.line().getId(), c.line().getInstructorUserId(),
                    c.studentName(), c.itemName(), c.line().getUsageType(), c.labName(),
                    c.booking().getProjectName(), c.booking().getStartDate(), c.booking().getReturnDate(),
                    c.line().getRequestedUseTime(), note));
        }
    }

    /** Stage 1: the department HoD rejects the line. */
    record HodReject(String reason) implements Transition {
        public Set<String> fromStates() { return Set.of(BookingState.AWAITING_HOD); }
        public String toState()         { return BookingState.HOD_REJECTED; }
        public Role requiredRole()      { return Role.HOD_ASSIGNED; }
        public List<NotificationEvent> notifications(TransitionContext c) {
            return List.of(new NotificationEvent.HodDeclinedToStudent(
                    c.booking().getId(), c.line().getId(), c.booking().getStudentUserId(),
                    c.itemName(), reason));
        }
    }

    record StartReview() implements Transition {
        public Set<String> fromStates()    { return Set.of(BookingState.SUBMITTED); }
        public String toState()            { return BookingState.INSTRUCTOR_REVIEWING; }
        public Role requiredRole()         { return Role.INSTRUCTOR_OWNER; }
        public boolean hasNotifications()  { return false; }
    }

    /** Borrowable: instructor approves with a pickup time; the item is held (IN_USE). */
    record ApproveDirectly(LocalDateTime pickupAt, String pickupNote) implements Transition {
        public Set<String> fromStates() { return Set.of(BookingState.SUBMITTED, BookingState.INSTRUCTOR_REVIEWING); }
        public String toState()         { return BookingState.READY_FOR_COLLECTION; }
        public Role requiredRole()      { return Role.INSTRUCTOR_OWNER; }
        public void mutate(BookingItem l) { l.setPickupAt(pickupAt); l.setPickupNote(pickupNote); }
        public Optional<String> itemStatusFlip(String fromState) { return Optional.of(ItemStatus.IN_USE); }
        public List<NotificationEvent> notifications(TransitionContext c) {
            return List.of(new NotificationEvent.ReadyForCollection(
                    c.booking().getId(), c.line().getId(), c.booking().getStudentUserId(),
                    c.itemName(), c.instructorName(),
                    c.instructor() != null ? c.instructor().getEmail() : "",
                    c.instructor() != null ? c.instructor().getPhoneNumber() : null,
                    pickupAt, pickupNote));
        }
    }

    /**
     * Lab-only: instructor confirms an available time for the student to use the item
     * in the lab. No collect/return lifecycle and the item is not globally locked —
     * other windows stay bookable; overlapping windows are still blocked at creation.
     */
    record ConfirmLab(LocalDateTime availableTime, String note) implements Transition {
        public Set<String> fromStates() { return Set.of(BookingState.SUBMITTED, BookingState.INSTRUCTOR_REVIEWING); }
        public String toState()         { return BookingState.LAB_CONFIRMED; }
        public Role requiredRole()      { return Role.INSTRUCTOR_OWNER; }
        public void mutate(BookingItem l) { l.setPickupAt(availableTime); l.setPickupNote(note); }
        public List<NotificationEvent> notifications(TransitionContext c) {
            return List.of(new NotificationEvent.LabConfirmed(
                    c.booking().getId(), c.line().getId(), c.booking().getStudentUserId(),
                    c.itemName(), c.labName(), c.instructorName(),
                    c.instructor() != null ? c.instructor().getEmail() : "",
                    c.instructor() != null ? c.instructor().getPhoneNumber() : null,
                    availableTime, note));
        }
    }

    record Reject(String reason) implements Transition {
        public Set<String> fromStates() { return Set.of(BookingState.SUBMITTED, BookingState.INSTRUCTOR_REVIEWING); }
        public String toState()         { return BookingState.INSTRUCTOR_REJECTED; }
        public Role requiredRole()      { return Role.INSTRUCTOR_OWNER; }
        public List<NotificationEvent> notifications(TransitionContext c) {
            return List.of(new NotificationEvent.ItemRejected(
                    c.booking().getId(), c.line().getId(), c.booking().getStudentUserId(),
                    c.itemName(), reason));
        }
    }

    record MarkCollected() implements Transition {
        public Set<String> fromStates()   { return Set.of(BookingState.READY_FOR_COLLECTION); }
        public String toState()           { return BookingState.COLLECTED; }
        public Role requiredRole()        { return Role.INSTRUCTOR_OWNER; }
        public boolean hasNotifications() { return false; }
    }

    record MarkReturned() implements Transition {
        public Set<String> fromStates()   { return Set.of(BookingState.COLLECTED, BookingState.OVERDUE); }
        public String toState()           { return BookingState.RETURNED; }
        public Role requiredRole()        { return Role.INSTRUCTOR_OWNER; }
        public boolean hasNotifications() { return false; }
        public Optional<String> itemStatusFlip(String fromState) { return Optional.of(ItemStatus.AVAILABLE); }
    }

    record FlipOverdue() implements Transition {
        public Set<String> fromStates() { return Set.of(BookingState.COLLECTED); }
        public String toState()         { return BookingState.OVERDUE; }
        public Role requiredRole()      { return Role.SYSTEM; }
        public List<NotificationEvent> notifications(TransitionContext c) {
            return List.of(
                    new NotificationEvent.OverdueToStudent(
                            c.booking().getId(), c.line().getId(), c.booking().getStudentUserId(),
                            c.itemName(), c.booking().getReturnDate()),
                    new NotificationEvent.OverdueToInstructor(
                            c.booking().getId(), c.line().getId(), c.line().getInstructorUserId(),
                            c.studentName(), c.itemName(), c.booking().getReturnDate()));
        }
    }

    /**
     * Per-line cancel. Booking-level orchestration (filter cancellable lines,
     * fire one BookingCancelled per unique instructor) lives in BookingService.
     */
    record Cancel() implements Transition {
        public Set<String> fromStates() {
            return Set.of(
                    BookingState.AWAITING_HOD, BookingState.SUBMITTED, BookingState.INSTRUCTOR_REVIEWING,
                    BookingState.READY_FOR_COLLECTION, BookingState.LAB_CONFIRMED);
        }
        public String toState()           { return BookingState.CANCELLED; }
        public Role requiredRole()        { return Role.STUDENT_OWNER; }
        public boolean hasNotifications() { return false; }
        public Optional<String> itemStatusFlip(String fromState) {
            // Only READY_FOR_COLLECTION held the item (IN_USE); release it on cancel.
            return BookingState.READY_FOR_COLLECTION.equals(fromState)
                    ? Optional.of(ItemStatus.AVAILABLE) : Optional.empty();
        }
    }
}
