package com.smartlab.bookingservice.transition;

/**
 * Who is allowed to drive a transition. Mapped to concrete user-role + ownership
 * checks by the engine; the Transition records just declare the slot.
 */
public enum Role {
    HOD_OF_STUDENT_DEPT, // user is HOD and heads the student's department (JWT departmentId match)
    HANDLER_ASSIGNED,    // user is staff (INSTRUCTOR/LECTURER/HOD) and is the line's assigned handler
    STUDENT_OWNER,       // user is STUDENT and is the booking's studentUserId
    SYSTEM               // cron / internal
}
