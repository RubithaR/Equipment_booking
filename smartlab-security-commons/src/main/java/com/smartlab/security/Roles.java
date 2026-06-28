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

    public static final Set<String> ALL = Set.of(
            MAIN_ADMIN, DEPT_ADMIN, HOD, LECTURER, INSTRUCTOR, STUDENT);

    /** Roles a user can pick during self-registration. */
    public static final Set<String> SELF_REGISTERABLE = Set.of(STUDENT, INSTRUCTOR);

    public static boolean isValid(String role) {
        return role != null && ALL.contains(role);
    }
}
