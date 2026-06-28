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
 * Wire shape: {@code { "type": "APPROVE_DIRECTLY", "pickupAt": "...", "pickupNote": "..." }}.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Transition.StartReview.class,        name = "START_REVIEW"),
        @JsonSubTypes.Type(value = Transition.ApproveDirectly.class,    name = "APPROVE_DIRECTLY"),
        @JsonSubTypes.Type(value = Transition.Reject.class,             name = "REJECT"),
        @JsonSubTypes.Type(value = Transition.Delegate.class,           name = "DELEGATE"),
        @JsonSubTypes.Type(value = Transition.Finalise.class,           name = "FINALISE"),
        @JsonSubTypes.Type(value = Transition.SupervisorApprove.class,  name = "SUPERVISOR_APPROVE"),
        @JsonSubTypes.Type(value = Transition.SupervisorDecline.class,  name = "SUPERVISOR_DECLINE"),
        @JsonSubTypes.Type(value = Transition.MarkCollected.class,      name = "MARK_COLLECTED"),
        @JsonSubTypes.Type(value = Transition.MarkReturned.class,       name = "MARK_RETURNED"),
        @JsonSubTypes.Type(value = Transition.FlipOverdue.class,        name = "FLIP_OVERDUE"),
        @JsonSubTypes.Type(value = Transition.Cancel.class,             name = "CANCEL"),
})
public sealed interface Transition permits
        Transition.StartReview,
        Transition.ApproveDirectly,
        Transition.Reject,
        Transition.Delegate,
        Transition.Finalise,
        Transition.SupervisorApprove,
        Transition.SupervisorDecline,
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

    record StartReview() implements Transition {
        public Set<String> fromStates()    { return Set.of(BookingState.SUBMITTED); }
        public String toState()            { return BookingState.INSTRUCTOR_REVIEWING; }
        public Role requiredRole()         { return Role.INSTRUCTOR_OWNER; }
        public boolean hasNotifications()  { return false; }
    }

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

    record Delegate(Long supervisorUserId, String note) implements Transition {
        public Set<String> fromStates() { return Set.of(BookingState.SUBMITTED, BookingState.INSTRUCTOR_REVIEWING); }
        public String toState()         { return BookingState.AWAITING_SUPERVISOR; }
        public Role requiredRole()      { return Role.INSTRUCTOR_OWNER; }
        public void mutate(BookingItem l) { l.setAssignedSupervisorUserId(supervisorUserId); }
        public List<NotificationEvent> notifications(TransitionContext c) {
            return List.of(
                    new NotificationEvent.DelegatedToSupervisor(
                            c.booking().getId(), c.line().getId(), supervisorUserId,
                            c.studentName(), c.itemName(), c.labName(),
                            c.booking().getProjectName(),
                            c.booking().getStartDate(), c.booking().getReturnDate(), note),
                    new NotificationEvent.DelegatedAckToStudent(
                            c.booking().getId(), c.booking().getStudentUserId(),
                            c.itemName(), c.supervisorName()));
        }
    }

    record Finalise(LocalDateTime pickupAt, String pickupNote) implements Transition {
        public Set<String> fromStates() { return Set.of(BookingState.SUPERVISOR_APPROVED); }
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

    record SupervisorApprove(String note) implements Transition {
        public Set<String> fromStates() { return Set.of(BookingState.AWAITING_SUPERVISOR); }
        public String toState()         { return BookingState.SUPERVISOR_APPROVED; }
        public Role requiredRole()      { return Role.SUPERVISOR_ASSIGNED; }
        public List<NotificationEvent> notifications(TransitionContext c) {
            return List.of(new NotificationEvent.SupervisorApproved(
                    c.booking().getId(), c.line().getId(), c.line().getInstructorUserId(),
                    c.studentName(), c.itemName(), c.supervisorName(), note));
        }
    }

    record SupervisorDecline(String note) implements Transition {
        public Set<String> fromStates() { return Set.of(BookingState.AWAITING_SUPERVISOR); }
        public String toState()         { return BookingState.SUPERVISOR_DECLINED; }
        public Role requiredRole()      { return Role.SUPERVISOR_ASSIGNED; }
        public List<NotificationEvent> notifications(TransitionContext c) {
            return List.of(
                    new NotificationEvent.SupervisorDeclinedToInstructor(
                            c.booking().getId(), c.line().getId(), c.line().getInstructorUserId(),
                            c.studentName(), c.itemName(), c.supervisorName(), note),
                    new NotificationEvent.SupervisorDeclinedToStudent(
                            c.booking().getId(), c.line().getId(), c.booking().getStudentUserId(),
                            c.itemName(), note));
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
                    BookingState.SUBMITTED, BookingState.INSTRUCTOR_REVIEWING,
                    BookingState.AWAITING_SUPERVISOR, BookingState.SUPERVISOR_APPROVED,
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
