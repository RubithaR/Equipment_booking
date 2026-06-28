package com.smartlab.bookingservice.entity;

import java.util.List;
import java.util.Set;

public final class BookingState {
    private BookingState() {}

    // ===== Per-item states (also used as umbrella states for single-item or homogeneous bookings) =====
    public static final String SUBMITTED             = "SUBMITTED";
    public static final String INSTRUCTOR_REVIEWING  = "INSTRUCTOR_REVIEWING";
    public static final String INSTRUCTOR_REJECTED   = "INSTRUCTOR_REJECTED";
    public static final String AWAITING_SUPERVISOR   = "AWAITING_SUPERVISOR";
    public static final String SUPERVISOR_APPROVED   = "SUPERVISOR_APPROVED";
    public static final String SUPERVISOR_DECLINED   = "SUPERVISOR_DECLINED";
    public static final String READY_FOR_COLLECTION  = "READY_FOR_COLLECTION";
    public static final String COLLECTED             = "COLLECTED";
    public static final String RETURNED              = "RETURNED";
    public static final String OVERDUE               = "OVERDUE";
    public static final String CANCELLED             = "CANCELLED";

    /** Umbrella-only label — used when items finished with a mix of outcomes. */
    public static final String COMPLETED             = "COMPLETED";

    /** States that hold the item — block other bookings on the same item for overlapping windows. */
    public static final Set<String> ACTIVE = Set.of(
            SUBMITTED, INSTRUCTOR_REVIEWING, AWAITING_SUPERVISOR, SUPERVISOR_APPROVED,
            READY_FOR_COLLECTION, COLLECTED, OVERDUE);

    /** Held, but still awaiting an approval decision — shown to students as "In process". */
    public static final Set<String> IN_PROCESS = Set.of(
            SUBMITTED, INSTRUCTOR_REVIEWING, AWAITING_SUPERVISOR);

    /** Approved and reserved for collection/use — shown to students as "In use". */
    public static final Set<String> IN_USE = Set.of(
            SUPERVISOR_APPROVED, READY_FOR_COLLECTION, COLLECTED, OVERDUE);

    /** Map a per-item state to the student-facing availability bucket. */
    public static String availabilityBucket(String state) {
        if (IN_USE.contains(state))     return "IN_USE";
        if (IN_PROCESS.contains(state)) return "IN_PROCESS";
        return "AVAILABLE";
    }

    /** Per-item terminal states. */
    public static final Set<String> TERMINAL = Set.of(
            INSTRUCTOR_REJECTED, SUPERVISOR_DECLINED, RETURNED, CANCELLED);

    /**
     * Priority order used when rolling per-item states up to an umbrella state — pick the
     * "earliest in the lifecycle" non-terminal state that any item is in. If every item is
     * terminal, return COMPLETED (or CANCELLED if all are CANCELLED).
     */
    private static final List<String> ROLLUP_PRIORITY = List.of(
            SUBMITTED,
            INSTRUCTOR_REVIEWING,
            AWAITING_SUPERVISOR,
            SUPERVISOR_APPROVED,
            READY_FOR_COLLECTION,
            COLLECTED,
            OVERDUE);

    public static boolean isActive(String state)   { return state != null && ACTIVE.contains(state); }
    public static boolean isTerminal(String state) { return state != null && TERMINAL.contains(state); }

    /** Cancellable while still pre-pickup; COLLECTED/OVERDUE must be returned, not cancelled. */
    public static boolean isCancellable(String state) {
        return isActive(state) && !COLLECTED.equals(state) && !OVERDUE.equals(state);
    }

    /** Roll a list of per-item states up to a single umbrella label. */
    public static String rollUp(List<String> itemStates) {
        if (itemStates == null || itemStates.isEmpty()) return SUBMITTED;
        for (String s : ROLLUP_PRIORITY) {
            if (itemStates.contains(s)) return s;
        }
        // Every item is terminal.
        boolean allCancelled = itemStates.stream().allMatch(CANCELLED::equals);
        if (allCancelled) return CANCELLED;
        return COMPLETED;
    }
}
