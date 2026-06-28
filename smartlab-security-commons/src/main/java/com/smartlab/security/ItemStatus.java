package com.smartlab.security;

import java.util.Set;

/** Equipment status values owned by equipment-service. Shared so callers (booking-service, frontends) reference them by identifier rather than string literal. */
public final class ItemStatus {
    private ItemStatus() {}

    public static final String AVAILABLE      = "AVAILABLE";
    public static final String IN_USE         = "IN_USE";
    public static final String MAINTENANCE    = "MAINTENANCE";
    public static final String OUT_OF_SERVICE = "OUT_OF_SERVICE";

    public static final Set<String> ALL = Set.of(AVAILABLE, IN_USE, MAINTENANCE, OUT_OF_SERVICE);

    public static boolean isValid(String s) {
        return s != null && ALL.contains(s);
    }

    /** True for statuses that block a booking from being created against an item. */
    public static boolean blocksBooking(String s) {
        return MAINTENANCE.equals(s) || OUT_OF_SERVICE.equals(s);
    }
}
