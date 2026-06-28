package com.smartlab.bookingservice.auth;

import com.smartlab.bookingservice.entity.Booking;
import com.smartlab.bookingservice.entity.BookingItem;
import com.smartlab.bookingservice.transition.Role;
import com.smartlab.security.Roles;
import com.smartlab.security.UserContext;
import com.smartlab.security.exception.AuthenticationException;
import com.smartlab.security.exception.AuthorizationException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exhaustive matrix of "who is allowed to do what" — these rules are the security
 * spine of booking-service and live nowhere else.
 */
class BookingAuthorizerTest {

    static final Long STUDENT_ID    = 100L;
    static final Long INSTRUCTOR_ID = 200L;
    static final Long SUPERVISOR_ID = 300L;
    static final Long OTHER_ID      = 999L;
    static final Long DEPT_ID       = 5L;
    static final Long OTHER_DEPT_ID = 6L;
    static final Long BOOKING_ID    = 1L;
    static final Long LAB_ID        = 30L;
    static final Long ITEM_ID       = 20L;

    BookingAuthorizer authorizer = new BookingAuthorizer();

    private Booking ownedByStudent() {
        Booking b = new Booking();
        b.setId(BOOKING_ID);
        b.setStudentUserId(STUDENT_ID);
        b.setStudentDepartmentId(DEPT_ID);
        return b;
    }

    private BookingItem lineFor(Long instructorId, Long supervisorId) {
        BookingItem li = new BookingItem();
        li.setId(10L);
        li.setBookingId(BOOKING_ID);
        li.setItemId(ITEM_ID);
        li.setLabId(LAB_ID);
        li.setInstructorUserId(instructorId);
        li.setAssignedSupervisorUserId(supervisorId);
        return li;
    }

    private UserContext user(Long id, String role, Long deptId) {
        return new UserContext(id, "u@uni.com", role, null, deptId);
    }

    // ===== requireCanRead =====

    @Nested
    class RequireCanRead {

        @Test
        void nullActor_throws401() {
            assertThatThrownBy(() -> authorizer.requireCanRead(null, ownedByStudent(), List.of()))
                    .isInstanceOf(AuthenticationException.class);
        }

        @Test
        void mainAdmin_canReadAnyBooking() {
            UserContext admin = user(OTHER_ID, Roles.MAIN_ADMIN, OTHER_DEPT_ID);
            authorizer.requireCanRead(admin, ownedByStudent(), List.of());
        }

        @Test
        void deptAdmin_canReadOwnDepartmentOnly() {
            UserContext sameDept = user(OTHER_ID, Roles.DEPT_ADMIN, DEPT_ID);
            authorizer.requireCanRead(sameDept, ownedByStudent(), List.of());

            UserContext otherDept = user(OTHER_ID, Roles.DEPT_ADMIN, OTHER_DEPT_ID);
            assertThatThrownBy(() -> authorizer.requireCanRead(otherDept, ownedByStudent(), List.of()))
                    .isInstanceOf(AuthorizationException.class);
        }

        @Test
        void owningStudent_canRead() {
            UserContext student = user(STUDENT_ID, Roles.STUDENT, DEPT_ID);
            authorizer.requireCanRead(student, ownedByStudent(), List.of());
        }

        @Test
        void instructorOnAnyLine_canRead() {
            UserContext instructor = user(INSTRUCTOR_ID, Roles.INSTRUCTOR, DEPT_ID);
            authorizer.requireCanRead(instructor, ownedByStudent(), List.of(lineFor(INSTRUCTOR_ID, null)));
        }

        @Test
        void assignedSupervisor_canRead() {
            UserContext supervisor = user(SUPERVISOR_ID, Roles.HOD, DEPT_ID);
            authorizer.requireCanRead(supervisor, ownedByStudent(),
                    List.of(lineFor(INSTRUCTOR_ID, SUPERVISOR_ID)));
        }

        @Test
        void unrelatedUser_cannotRead() {
            UserContext stranger = user(OTHER_ID, Roles.STUDENT, DEPT_ID);
            assertThatThrownBy(() -> authorizer.requireCanRead(stranger, ownedByStudent(),
                    List.of(lineFor(INSTRUCTOR_ID, null))))
                    .isInstanceOf(AuthorizationException.class);
        }
    }

    // ===== requireCanCancel =====

    @Nested
    class RequireCanCancel {

        @Test
        void nullActor_throws401() {
            assertThatThrownBy(() -> authorizer.requireCanCancel(null, ownedByStudent()))
                    .isInstanceOf(AuthenticationException.class);
        }

        @Test
        void owningStudent_canCancel() {
            authorizer.requireCanCancel(user(STUDENT_ID, Roles.STUDENT, DEPT_ID), ownedByStudent());
        }

        @Test
        void differentStudent_cannotCancel() {
            assertThatThrownBy(() -> authorizer.requireCanCancel(user(OTHER_ID, Roles.STUDENT, DEPT_ID), ownedByStudent()))
                    .isInstanceOf(AuthorizationException.class);
        }

        @Test
        void mainAdmin_cannotCancel_onlyStudentOwnerCan() {
            assertThatThrownBy(() -> authorizer.requireCanCancel(user(OTHER_ID, Roles.MAIN_ADMIN, DEPT_ID), ownedByStudent()))
                    .isInstanceOf(AuthorizationException.class);
        }
    }

    // ===== requireForTransition =====

    @Nested
    class RequireForTransition {

        @Test
        void systemRole_alwaysPasses_evenWithNullActor() {
            BookingItem li = lineFor(INSTRUCTOR_ID, null);
            authorizer.requireForTransition(null, li, ownedByStudent(), Role.SYSTEM);
        }

        @Test
        void nullActor_with_anyOtherRole_throws401() {
            BookingItem li = lineFor(INSTRUCTOR_ID, null);
            assertThatThrownBy(() -> authorizer.requireForTransition(null, li, ownedByStudent(), Role.INSTRUCTOR_OWNER))
                    .isInstanceOf(AuthenticationException.class);
        }

        @Test
        void instructorOwner_correctInstructor_passes() {
            BookingItem li = lineFor(INSTRUCTOR_ID, null);
            authorizer.requireForTransition(user(INSTRUCTOR_ID, Roles.INSTRUCTOR, DEPT_ID), li, ownedByStudent(), Role.INSTRUCTOR_OWNER);
        }

        @Test
        void instructorOwner_wrongRole_throws403() {
            BookingItem li = lineFor(INSTRUCTOR_ID, null);
            assertThatThrownBy(() -> authorizer.requireForTransition(
                    user(INSTRUCTOR_ID, Roles.STUDENT, DEPT_ID), li, ownedByStudent(), Role.INSTRUCTOR_OWNER))
                    .isInstanceOf(AuthorizationException.class)
                    .hasMessageContaining("Only instructors");
        }

        @Test
        void instructorOwner_differentInstructor_throws403() {
            BookingItem li = lineFor(INSTRUCTOR_ID, null);
            assertThatThrownBy(() -> authorizer.requireForTransition(
                    user(OTHER_ID, Roles.INSTRUCTOR, DEPT_ID), li, ownedByStudent(), Role.INSTRUCTOR_OWNER))
                    .isInstanceOf(AuthorizationException.class)
                    .hasMessageContaining("different instructor");
        }

        @Test
        void supervisorAssigned_HoD_passes() {
            BookingItem li = lineFor(INSTRUCTOR_ID, SUPERVISOR_ID);
            authorizer.requireForTransition(user(SUPERVISOR_ID, Roles.HOD, DEPT_ID), li, ownedByStudent(), Role.SUPERVISOR_ASSIGNED);
        }

        @Test
        void supervisorAssigned_Lecturer_passes() {
            BookingItem li = lineFor(INSTRUCTOR_ID, SUPERVISOR_ID);
            authorizer.requireForTransition(user(SUPERVISOR_ID, Roles.LECTURER, DEPT_ID), li, ownedByStudent(), Role.SUPERVISOR_ASSIGNED);
        }

        @Test
        void supervisorAssigned_studentRole_throws403() {
            BookingItem li = lineFor(INSTRUCTOR_ID, SUPERVISOR_ID);
            assertThatThrownBy(() -> authorizer.requireForTransition(
                    user(SUPERVISOR_ID, Roles.STUDENT, DEPT_ID), li, ownedByStudent(), Role.SUPERVISOR_ASSIGNED))
                    .isInstanceOf(AuthorizationException.class);
        }

        @Test
        void supervisorAssigned_butNoSupervisorAssignedToLine_throws403() {
            BookingItem li = lineFor(INSTRUCTOR_ID, null);
            assertThatThrownBy(() -> authorizer.requireForTransition(
                    user(SUPERVISOR_ID, Roles.HOD, DEPT_ID), li, ownedByStudent(), Role.SUPERVISOR_ASSIGNED))
                    .isInstanceOf(AuthorizationException.class)
                    .hasMessageContaining("not the assigned supervisor");
        }

        @Test
        void supervisorAssigned_differentSupervisor_throws403() {
            BookingItem li = lineFor(INSTRUCTOR_ID, SUPERVISOR_ID);
            assertThatThrownBy(() -> authorizer.requireForTransition(
                    user(OTHER_ID, Roles.HOD, DEPT_ID), li, ownedByStudent(), Role.SUPERVISOR_ASSIGNED))
                    .isInstanceOf(AuthorizationException.class);
        }

        @Test
        void studentOwner_owningStudent_passes() {
            BookingItem li = lineFor(INSTRUCTOR_ID, null);
            authorizer.requireForTransition(user(STUDENT_ID, Roles.STUDENT, DEPT_ID), li, ownedByStudent(), Role.STUDENT_OWNER);
        }

        @Test
        void studentOwner_differentStudent_throws403() {
            BookingItem li = lineFor(INSTRUCTOR_ID, null);
            assertThatThrownBy(() -> authorizer.requireForTransition(
                    user(OTHER_ID, Roles.STUDENT, DEPT_ID), li, ownedByStudent(), Role.STUDENT_OWNER))
                    .isInstanceOf(AuthorizationException.class)
                    .hasMessageContaining("different student");
        }
    }

    // ===== requireAdminListAccess =====

    @Nested
    class RequireAdminListAccess {

        @Test
        void mainAdmin_passes() {
            authorizer.requireAdminListAccess(user(OTHER_ID, Roles.MAIN_ADMIN, DEPT_ID));
        }

        @Test
        void deptAdmin_passes() {
            authorizer.requireAdminListAccess(user(OTHER_ID, Roles.DEPT_ADMIN, DEPT_ID));
        }

        @Test
        void student_throws403() {
            assertThatThrownBy(() -> authorizer.requireAdminListAccess(user(STUDENT_ID, Roles.STUDENT, DEPT_ID)))
                    .isInstanceOf(AuthorizationException.class);
        }

        @Test
        void nullActor_throws403() {
            assertThatThrownBy(() -> authorizer.requireAdminListAccess(null))
                    .isInstanceOf(AuthorizationException.class);
        }
    }

    // ===== listScopeDepartmentId =====

    @Test
    void listScope_deptAdmin_returnsTheirDepartment() {
        assertThat(authorizer.listScopeDepartmentId(user(OTHER_ID, Roles.DEPT_ADMIN, DEPT_ID)))
                .contains(DEPT_ID);
    }

    @Test
    void listScope_mainAdmin_returnsEmpty() {
        assertThat(authorizer.listScopeDepartmentId(user(OTHER_ID, Roles.MAIN_ADMIN, DEPT_ID)))
                .isEmpty();
    }

    @Test
    void listScope_nonAdmin_returnsEmpty() {
        assertThat(authorizer.listScopeDepartmentId(user(STUDENT_ID, Roles.STUDENT, DEPT_ID)))
                .isEmpty();
    }
}
