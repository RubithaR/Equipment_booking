package com.smartlab.security;

import java.util.Set;

/** Canonical role names carried in JWT claims and enforced by the security filter. */
public final class Roles {
    private Roles() {}

    public static final String MAIN_ADMIN = "MAIN_ADMIN";
    public static final String DEPT_ADMIN = "DEPT_ADMIN";
    public static final String HOD        = "HOD";
    public static final String LECTURER   = "LECTURER";
    public static final String INSTRUCTOR = "INSTRUCTOR";
    public static final String STUDENT    = "STUDENT";

    /** Placeholder role for a registered staff member whose real role hasn't been assigned yet. */
    public static final String STAFF      = "STAFF";

    public static final Set<String> ALL = Set.of(
            MAIN_ADMIN, DEPT_ADMIN, HOD, LECTURER, INSTRUCTOR, STUDENT, STAFF);

    /** Real staff roles an admin can assign to a registered {@link #STAFF} account. */
    public static final Set<String> ASSIGNABLE_STAFF = Set.of(HOD, LECTURER, INSTRUCTOR);

    /** Roles a user can pick during self-registration: a student, or an unassigned staff member. */
    public static final Set<String> SELF_REGISTERABLE = Set.of(STUDENT, STAFF);

    public static boolean isValid(String role) {
        return role != null && ALL.contains(role);
    }
}
