package com.smartlab.bookingservice.entity;

import java.util.List;
import java.util.Set;

public final class BookingState {
    private BookingState() {}

    // ===== Per-item states (also used as umbrella states for single-item or homogeneous bookings) =====
    /** Stage 1: routed to the department HoD, awaiting their decision. */
    public static final String AWAITING_HOD          = "AWAITING_HOD";
    /** Terminal: the department HoD rejected the line. */
    public static final String HOD_REJECTED          = "HOD_REJECTED";
    /** Stage 2: HoD approved; now with the lab instructor (umbrella entry was SUBMITTED). */
    public static final String SUBMITTED             = "SUBMITTED";
    public static final String INSTRUCTOR_REVIEWING  = "INSTRUCTOR_REVIEWING";
    public static final String INSTRUCTOR_REJECTED   = "INSTRUCTOR_REJECTED";
    /** Borrowable: instructor approved with a pickup time; item is held. */
    public static final String READY_FOR_COLLECTION  = "READY_FOR_COLLECTION";
    /** Lab-only: instructor confirmed an available time; student uses it in the lab. */
    public static final String LAB_CONFIRMED         = "LAB_CONFIRMED";
    public static final String COLLECTED             = "COLLECTED";
    public static final String RETURNED              = "RETURNED";
    public static final String OVERDUE               = "OVERDUE";
    public static final String CANCELLED             = "CANCELLED";

    // Legacy states from the old instructor->supervisor delegation. Kept so historical
    // rows and events still resolve; no transition produces them any more.
    public static final String AWAITING_SUPERVISOR   = "AWAITING_SUPERVISOR";
    public static final String SUPERVISOR_APPROVED   = "SUPERVISOR_APPROVED";
    public static final String SUPERVISOR_DECLINED   = "SUPERVISOR_DECLINED";

    /** Umbrella-only label — used when items finished with a mix of outcomes. */
    public static final String COMPLETED             = "COMPLETED";

    /** States that hold the item — block other bookings on the same item for overlapping windows. */
    public static final Set<String> ACTIVE = Set.of(
            AWAITING_HOD, SUBMITTED, INSTRUCTOR_REVIEWING,
            READY_FOR_COLLECTION, LAB_CONFIRMED, COLLECTED, OVERDUE);

    /** Per-item terminal states. */
    public static final Set<String> TERMINAL = Set.of(
            HOD_REJECTED, INSTRUCTOR_REJECTED, RETURNED, CANCELLED);

    /**
     * Priority order used when rolling per-item states up to an umbrella state — pick the
     * "earliest in the lifecycle" non-terminal state that any item is in. If every item is
     * terminal, return COMPLETED (or CANCELLED if all are CANCELLED).
     */
    private static final List<String> ROLLUP_PRIORITY = List.of(
            AWAITING_HOD,
            SUBMITTED,
            INSTRUCTOR_REVIEWING,
            READY_FOR_COLLECTION,
            LAB_CONFIRMED,
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
