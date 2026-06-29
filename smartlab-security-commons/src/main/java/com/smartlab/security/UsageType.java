package com.smartlab.security;

import java.util.Set;

/**
 * How an item may be used. Owned by equipment-service, shared so booking-service
 * and the frontends reference the values by identifier rather than string literal.
 *
 * <ul>
 *   <li>{@code BORROWABLE} — the student takes the item out of the lab; it runs the
 *       collect → return lifecycle and is locked (IN_USE) for the loan window.</li>
 *   <li>{@code LAB_ONLY} — the item stays in the lab; the instructor confirms an
 *       available time and the student uses it in the lab at that time. No
 *       collect/return.</li>
 * </ul>
 */
public final class UsageType {
    private UsageType() {}

    public static final String BORROWABLE = "BORROWABLE";
    public static final String LAB_ONLY   = "LAB_ONLY";

    public static final Set<String> ALL = Set.of(BORROWABLE, LAB_ONLY);

    public static boolean isValid(String s) {
        return s != null && ALL.contains(s);
    }

    public static boolean isLabOnly(String s) {
        return LAB_ONLY.equals(s);
    }
}
