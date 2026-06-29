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

    /** Teaching/lab staff who self-register and go live after department admin approval. */
    public static final Set<String> STAFF = Set.of(HOD, LECTURER, INSTRUCTOR);

    /** Roles a user can pick during self-registration: a student, or any staff member. */
    public static final Set<String> SELF_REGISTERABLE = Set.of(STUDENT, HOD, LECTURER, INSTRUCTOR);

    public static boolean isValid(String role) {
        return role != null && ALL.contains(role);
    }
}
