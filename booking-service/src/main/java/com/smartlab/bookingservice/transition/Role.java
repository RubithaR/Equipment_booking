package com.smartlab.bookingservice.transition;

/**
 * Who is allowed to drive a transition. Mapped to concrete user-role + ownership
 * checks by the engine; the Transition records just declare the slot.
 */
public enum Role {
    INSTRUCTOR_OWNER,    // user is INSTRUCTOR and is the line's instructorUserId
    SUPERVISOR_ASSIGNED, // user is HOD or LECTURER and is the line's assignedSupervisorUserId
    STUDENT_OWNER,       // user is STUDENT and is the booking's studentUserId
    SYSTEM               // cron / internal
}
