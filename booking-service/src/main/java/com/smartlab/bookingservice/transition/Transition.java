package com.smartlab.bookingservice.transition;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.smartlab.bookingservice.entity.BookingItem;
import com.smartlab.bookingservice.entity.BookingState;
import com.smartlab.bookingservice.entity.UseSlot;
import com.smartlab.security.ItemStatus;
import com.smartlab.security.UsageType;
import com.smartlab.security.exception.BadRequestException;
import com.smartlab.bookingservice.notifier.NotificationEvent;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The whole booking-item state machine in one closed set of values. Each record
 * declares the rule for its transition: from-states, to-state, who's allowed,
 * what to mutate, whether to flip the underlying item, and which notifications
 * to fire. The engine runs the same skeleton for every one of them.
 *
 * Flow: SUBMITTED --(HOD approve, assigns handler)--> AWAITING_HANDLER
 *       --(handler approve)--> READY_FOR_COLLECTION --> COLLECTED --> RETURNED.
 * Either the HOD or the handler can REJECT; the student can CANCEL pre-pickup.
 *
 * Wire shape: {@code { "type": "HOD_APPROVE", "handlerUserId": 12, "note": "..." }}.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Transition.HodApprove.class,      name = "HOD_APPROVE"),
        @JsonSubTypes.Type(value = Transition.HodReject.class,       name = "HOD_REJECT"),
        @JsonSubTypes.Type(value = Transition.HandlerApprove.class,  name = "HANDLER_APPROVE"),
        @JsonSubTypes.Type(value = Transition.HandlerReject.class,   name = "HANDLER_REJECT"),
        @JsonSubTypes.Type(value = Transition.MarkCollected.class,   name = "MARK_COLLECTED"),
        @JsonSubTypes.Type(value = Transition.MarkReturned.class,    name = "MARK_RETURNED"),
        @JsonSubTypes.Type(value = Transition.FlipOverdue.class,     name = "FLIP_OVERDUE"),
        @JsonSubTypes.Type(value = Transition.Cancel.class,          name = "CANCEL"),
})
public sealed interface Transition permits
        Transition.HodApprove,
        Transition.HodReject,
        Transition.HandlerApprove,
        Transition.HandlerReject,
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

    /** The student's department HOD approves and assigns a handler (instructor / lecturer / HOD). */
    record HodApprove(Long handlerUserId, String note) implements Transition {
        public Set<String> fromStates() { return Set.of(BookingState.SUBMITTED); }
        public String toState()         { return BookingState.AWAITING_HANDLER; }
        public Role requiredRole()      { return Role.HOD_OF_STUDENT_DEPT; }
        public void mutate(BookingItem l) { l.setInstructorUserId(handlerUserId); }
        public List<NotificationEvent> notifications(TransitionContext c) {
            // HoD approved and assigned a handler — notify that handler (the line's instructorUserId).
            return List.of(new NotificationEvent.HodApprovedToInstructor(
                    c.booking().getId(), c.line().getId(), c.line().getInstructorUserId(),
                    c.studentName(), c.itemName(), c.line().getUsageType(),
                    c.labName(), c.booking().getProjectName(),
                    c.booking().getStartDate(), c.booking().getReturnDate(),
                    c.line().getRequestedUseTime(), note));
        }
    }

    record HodReject(String reason) implements Transition {
        public Set<String> fromStates() { return Set.of(BookingState.SUBMITTED); }
        public String toState()         { return BookingState.REJECTED; }
        public Role requiredRole()      { return Role.HOD_OF_STUDENT_DEPT; }
        public List<NotificationEvent> notifications(TransitionContext c) {
            return List.of(new NotificationEvent.ItemRejected(
                    c.booking().getId(), c.line().getId(), c.booking().getStudentUserId(),
                    c.itemName(), reason));
        }
    }

    /**
     * The assigned handler approves. Borrowable items get a pickup time. LAB_ONLY
     * items get the times the handler ticked from the student's proposed slots —
     * those become the confirmed lab-use times shown back to the student.
     */
    record HandlerApprove(LocalDateTime pickupAt, String pickupNote, List<LocalDateTime> confirmedSlots)
            implements Transition {
        public Set<String> fromStates() { return Set.of(BookingState.AWAITING_HANDLER); }
        public String toState()         { return BookingState.READY_FOR_COLLECTION; }
        public Role requiredRole()      { return Role.HANDLER_ASSIGNED; }
        public void mutate(BookingItem l) {
            if (UsageType.LAB_ONLY.equals(l.getUsageType())) {
                if (confirmedSlots == null || confirmedSlots.isEmpty()) {
                    throw new BadRequestException("Tick at least one available time to confirm.");
                }
                List<UseSlot> slots = l.getUseSlots() != null ? l.getUseSlots() : new ArrayList<>();
                for (UseSlot s : slots) {
                    s.setConfirmed(confirmedSlots.contains(s.getAt()));
                }
                // Legacy / no proposed slots: adopt the handler's confirmed times directly.
                if (slots.stream().noneMatch(UseSlot::isConfirmed)) {
                    slots = new ArrayList<>();
                    for (LocalDateTime t : confirmedSlots) slots.add(new UseSlot(t, true));
                }
                l.setUseSlots(slots);
                l.setPickupAt(slots.stream().filter(UseSlot::isConfirmed)
                        .map(UseSlot::getAt).min(LocalDateTime::compareTo).orElse(null));
                l.setPickupNote(pickupNote);
            } else {
                l.setPickupAt(pickupAt);
                l.setPickupNote(pickupNote);
            }
        }
        public Optional<String> itemStatusFlip(String fromState) { return Optional.of(ItemStatus.IN_USE); }
        public List<NotificationEvent> notifications(TransitionContext c) {
            return List.of(new NotificationEvent.ReadyForCollection(
                    c.booking().getId(), c.line().getId(), c.booking().getStudentUserId(),
                    c.itemName(), c.instructorName(),
                    c.instructor() != null ? c.instructor().getEmail() : "",
                    c.instructor() != null ? c.instructor().getPhoneNumber() : null,
                    c.line().getPickupAt(), pickupNote));
        }
    }

    record HandlerReject(String reason) implements Transition {
        public Set<String> fromStates() { return Set.of(BookingState.AWAITING_HANDLER); }
        public String toState()         { return BookingState.REJECTED; }
        public Role requiredRole()      { return Role.HANDLER_ASSIGNED; }
        public List<NotificationEvent> notifications(TransitionContext c) {
            return List.of(new NotificationEvent.ItemRejected(
                    c.booking().getId(), c.line().getId(), c.booking().getStudentUserId(),
                    c.itemName(), reason));
        }
    }

    record MarkCollected() implements Transition {
        public Set<String> fromStates()   { return Set.of(BookingState.READY_FOR_COLLECTION); }
        public String toState()           { return BookingState.COLLECTED; }
        public Role requiredRole()        { return Role.HANDLER_ASSIGNED; }
        public boolean hasNotifications() { return false; }
    }

    record MarkReturned() implements Transition {
        public Set<String> fromStates()   { return Set.of(BookingState.COLLECTED, BookingState.OVERDUE); }
        public String toState()           { return BookingState.RETURNED; }
        public Role requiredRole()        { return Role.HANDLER_ASSIGNED; }
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
     * fire one BookingCancelled per unique handler) lives in BookingService.
     */
    record Cancel() implements Transition {
        public Set<String> fromStates() {
            return Set.of(
                    BookingState.SUBMITTED, BookingState.AWAITING_HANDLER,
                    BookingState.READY_FOR_COLLECTION);
        }
        public String toState()           { return BookingState.CANCELLED; }
        public Role requiredRole()        { return Role.STUDENT_OWNER; }
        public boolean hasNotifications() { return false; }
        public Optional<String> itemStatusFlip(String fromState) {
            return BookingState.READY_FOR_COLLECTION.equals(fromState)
                    ? Optional.of(ItemStatus.AVAILABLE) : Optional.empty();
        }
    }
}
