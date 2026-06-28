package com.smartlab.bookingservice.auth;

import com.smartlab.bookingservice.entity.Booking;
import com.smartlab.bookingservice.entity.BookingItem;
import com.smartlab.security.exception.AuthenticationException;
import com.smartlab.security.exception.AuthorizationException;
import com.smartlab.security.Roles;
import com.smartlab.security.UserContext;
import com.smartlab.bookingservice.transition.Role;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Single home for "can this user do this thing to this booking?". The transition
 * engine, the read paths, and the cancel orchestrator all consult it; the rules
 * for instructor / supervisor / student / admin scoping live here, not scattered.
 */
@Component
public class BookingAuthorizer {

    /** Throws if the actor cannot view this booking. */
    public void requireCanRead(UserContext actor, Booking booking, List<BookingItem> lines) {
        if (actor == null) throw new AuthenticationException("Authentication required");
        if (isMainAdmin(actor)) return;
        if (isDeptAdmin(actor)) {
            if (booking.getStudentDepartmentId() != null
                    && booking.getStudentDepartmentId().equals(actor.departmentId())) return;
            throw new AuthorizationException("Department admins only see their department's bookings");
        }
        if (actor.userId().equals(booking.getStudentUserId())) return;
        for (BookingItem line : lines) {
            if (actor.userId().equals(line.getInstructorUserId())) return;
            if (line.getAssignedSupervisorUserId() != null
                    && actor.userId().equals(line.getAssignedSupervisorUserId())) return;
        }
        throw new AuthorizationException("You don't have access to booking #" + booking.getId());
    }

    /** Throws if the actor cannot cancel this booking (must be the owning student). */
    public void requireCanCancel(UserContext actor, Booking booking) {
        if (actor == null) throw new AuthenticationException("Authentication required");
        if (!actor.userId().equals(booking.getStudentUserId())) {
            throw new AuthorizationException("Only the student who owns this booking can cancel it");
        }
    }

    /** Throws if the actor isn't allowed to drive a transition with this required role. */
    public void requireForTransition(UserContext actor, BookingItem line, Booking booking, Role required) {
        if (required == Role.SYSTEM) return;
        if (actor == null) throw new AuthenticationException("Authentication required");
        switch (required) {
            case INSTRUCTOR_OWNER -> {
                if (!actor.hasRole(Roles.INSTRUCTOR)) {
                    throw new AuthorizationException("Only instructors can take this action");
                }
                if (!actor.userId().equals(line.getInstructorUserId())) {
                    throw new AuthorizationException("This line belongs to a different instructor");
                }
            }
            case SUPERVISOR_ASSIGNED -> {
                if (!actor.hasAnyRole(Roles.HOD, Roles.LECTURER)) {
                    throw new AuthorizationException("Only HoDs and Lecturers can take this action");
                }
                if (line.getAssignedSupervisorUserId() == null
                        || !actor.userId().equals(line.getAssignedSupervisorUserId())) {
                    throw new AuthorizationException("You are not the assigned supervisor for this line");
                }
            }
            case STUDENT_OWNER -> {
                if (!actor.hasRole(Roles.STUDENT)) {
                    throw new AuthorizationException("Only the student can take this action");
                }
                if (!actor.userId().equals(booking.getStudentUserId())) {
                    throw new AuthorizationException("This booking belongs to a different student");
                }
            }
            default -> throw new AuthorizationException("Unsupported role: " + required);
        }
    }

    /** Admin-only entry to the booking list. Throws if the actor isn't any flavour of admin. */
    public void requireAdminListAccess(UserContext actor) {
        if (actor == null || !isAnyAdmin(actor)) {
            throw new AuthorizationException("Admin only");
        }
    }

    /**
     * If the actor is a Department Admin, their views/queries should be filtered
     * to that department. Main Admins see everything (empty). Other roles
     * shouldn't reach the admin list at all — guard with {@link #requireAdminListAccess}.
     */
    public Optional<Long> listScopeDepartmentId(UserContext actor) {
        if (actor != null && isDeptAdmin(actor)) {
            return Optional.ofNullable(actor.departmentId());
        }
        return Optional.empty();
    }

    public boolean isMainAdmin(UserContext actor) { return actor != null && actor.hasRole(Roles.MAIN_ADMIN); }
    public boolean isDeptAdmin(UserContext actor) { return actor != null && actor.hasRole(Roles.DEPT_ADMIN); }
    public boolean isAnyAdmin(UserContext actor)  { return actor != null && actor.hasAnyRole(Roles.MAIN_ADMIN, Roles.DEPT_ADMIN); }
}
