package com.smartlab.bookingservice.transition;

import com.smartlab.bookingservice.auth.BookingAuthorizer;
import com.smartlab.bookingservice.client.ItemClient;
import com.smartlab.bookingservice.client.LabClient;
import com.smartlab.bookingservice.client.UserClient;
import com.smartlab.bookingservice.dto.ItemDto;
import com.smartlab.bookingservice.dto.LabDto;
import com.smartlab.bookingservice.dto.UserDto;
import com.smartlab.bookingservice.entity.Booking;
import com.smartlab.bookingservice.entity.BookingItem;
import com.smartlab.bookingservice.entity.BookingState;
import com.smartlab.security.ItemStatus;
import com.smartlab.bookingservice.notifier.NotificationEvent;
import com.smartlab.notificationclient.InMemoryNotifier;
import com.smartlab.bookingservice.repository.BookingEventRepository;
import com.smartlab.bookingservice.repository.BookingItemRepository;
import com.smartlab.bookingservice.repository.BookingRepository;
import com.smartlab.security.UserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransitionEngineTest {

    @Mock BookingRepository       bookingRepository;
    @Mock BookingItemRepository   itemRepository;
    @Mock BookingEventRepository  eventRepository;
    @Mock UserClient              userClient;
    @Mock ItemClient              itemClient;
    @Mock LabClient               labClient;

    InMemoryNotifier<NotificationEvent> notifier = new InMemoryNotifier<>();
    BookingAuthorizer authorizer = new BookingAuthorizer();

    TransitionEngine engine;

    static final Long BOOKING_ID    = 1L;
    static final Long LINE_ID       = 10L;
    static final Long ITEM_ID       = 20L;
    static final Long LAB_ID        = 30L;
    static final Long STUDENT_ID    = 100L;
    static final Long INSTRUCTOR_ID = 200L;
    static final Long SUPERVISOR_ID = 300L;
    static final Long DEPT_ID       = 5L;

    @BeforeEach
    void setUp() {
        engine = new TransitionEngine(bookingRepository, itemRepository, eventRepository,
                userClient, itemClient, labClient, notifier, authorizer);
        notifier.clear();
    }

    // ===== helpers =====

    private Booking booking(String state) {
        Booking b = new Booking();
        b.setId(BOOKING_ID);
        b.setStudentUserId(STUDENT_ID);
        b.setStudentDepartmentId(DEPT_ID);
        b.setProjectName("Project X");
        b.setPurpose("Research");
        b.setStartDate(LocalDateTime.now().plusDays(1));
        b.setReturnDate(LocalDateTime.now().plusDays(3));
        b.setState(state);
        return b;
    }

    private BookingItem line(String state) {
        BookingItem li = new BookingItem();
        li.setId(LINE_ID);
        li.setBookingId(BOOKING_ID);
        li.setItemId(ITEM_ID);
        li.setLabId(LAB_ID);
        li.setInstructorUserId(INSTRUCTOR_ID);
        li.setState(state);
        return li;
    }

    private BookingItem lineWithSupervisor(String state) {
        BookingItem li = line(state);
        li.setAssignedSupervisorUserId(SUPERVISOR_ID);
        return li;
    }

    private UserContext instructor() {
        return new UserContext(INSTRUCTOR_ID, "instructor@lab.com", "INSTRUCTOR", null, DEPT_ID);
    }

    private UserContext student() {
        return new UserContext(STUDENT_ID, "student@uni.com", "STUDENT", null, DEPT_ID);
    }

    private UserContext supervisor() {
        return new UserContext(SUPERVISOR_ID, "hod@uni.com", "HOD", null, DEPT_ID);
    }

    private void stubSave(Booking b) {
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(b));
        when(itemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(eventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private void stubRollup(String lineState) {
        BookingItem li = new BookingItem();
        li.setState(lineState);
        when(itemRepository.findByBookingIdOrderByIdAsc(BOOKING_ID)).thenReturn(List.of(li));
    }

    private void stubContextLookups() {
        UserDto student    = new UserDto(); student.setFullName("Alice");    student.setId(STUDENT_ID);
        UserDto instructor = new UserDto(); instructor.setFullName("Bob");   instructor.setId(INSTRUCTOR_ID);
        UserDto supervisor = new UserDto(); supervisor.setFullName("Carol"); supervisor.setId(SUPERVISOR_ID);
        ItemDto item = new ItemDto(); item.setId(ITEM_ID); item.setName("Oscilloscope"); item.setLabId(LAB_ID);
        LabDto  lab  = new LabDto();  lab.setId(LAB_ID);  lab.setName("Electronics Lab");

        when(userClient.getUserById(STUDENT_ID)).thenReturn(student);
        when(userClient.getUserById(INSTRUCTOR_ID)).thenReturn(instructor);
        lenient().when(userClient.getUserById(SUPERVISOR_ID)).thenReturn(supervisor);
        when(itemClient.getItemById(ITEM_ID)).thenReturn(item);
        when(labClient.getLabById(LAB_ID)).thenReturn(lab);
    }

    // ===== StartReview =====

    @Test
    void startReview_movesLineToInstructorReviewing_noNotificationsNoFeign() {
        Booking b  = booking(BookingState.SUBMITTED);
        BookingItem li = line(BookingState.SUBMITTED);
        stubSave(b);
        stubRollup(BookingState.INSTRUCTOR_REVIEWING);

        BookingItem result = engine.apply(li, new Transition.StartReview(), instructor());

        assertThat(result.getState()).isEqualTo(BookingState.INSTRUCTOR_REVIEWING);
        assertThat(notifier.published()).isEmpty();
        verifyNoInteractions(userClient, itemClient, labClient);
    }

    // ===== ApproveDirectly =====

    @Test
    void approveDirectly_fromSubmitted_setsPickupAndNotifiesStudent() {
        Booking b  = booking(BookingState.SUBMITTED);
        BookingItem li = line(BookingState.SUBMITTED);
        LocalDateTime pickup = LocalDateTime.now().plusDays(1);
        stubSave(b);
        stubRollup(BookingState.READY_FOR_COLLECTION);
        stubContextLookups();
        when(itemClient.updateStatus(eq(ITEM_ID), any())).thenReturn(new ItemDto());

        BookingItem result = engine.apply(li, new Transition.ApproveDirectly(pickup, "Bring your ID"), instructor());

        assertThat(result.getState()).isEqualTo(BookingState.READY_FOR_COLLECTION);
        assertThat(result.getPickupAt()).isEqualTo(pickup);
        assertThat(result.getPickupNote()).isEqualTo("Bring your ID");
        verify(itemClient).updateStatus(eq(ITEM_ID), eq(Map.of("status", ItemStatus.IN_USE)));

        assertThat(notifier.published()).hasSize(1);
        assertThat(notifier.published().get(0)).isInstanceOf(NotificationEvent.ReadyForCollection.class);
        NotificationEvent.ReadyForCollection ev = (NotificationEvent.ReadyForCollection) notifier.published().get(0);
        assertThat(ev.studentUserId()).isEqualTo(STUDENT_ID);
    }

    // ===== Reject =====

    @Test
    void reject_movesLineToRejected_notifiesStudent() {
        Booking b  = booking(BookingState.INSTRUCTOR_REVIEWING);
        BookingItem li = line(BookingState.INSTRUCTOR_REVIEWING);
        stubSave(b);
        stubRollup(BookingState.INSTRUCTOR_REJECTED);
        stubContextLookups();

        BookingItem result = engine.apply(li, new Transition.Reject("Wrong lab"), instructor());

        assertThat(result.getState()).isEqualTo(BookingState.INSTRUCTOR_REJECTED);
        assertThat(notifier.published()).hasSize(1);
        NotificationEvent.ItemRejected ev = (NotificationEvent.ItemRejected) notifier.published().get(0);
        assertThat(ev.studentUserId()).isEqualTo(STUDENT_ID);
        assertThat(ev.reason()).isEqualTo("Wrong lab");
    }

    // ===== Delegate =====

    @Test
    void delegate_setsAssignedSupervisorAndNotifiesBothParties() {
        Booking b  = booking(BookingState.INSTRUCTOR_REVIEWING);
        BookingItem li = line(BookingState.INSTRUCTOR_REVIEWING);
        stubSave(b);
        stubRollup(BookingState.AWAITING_SUPERVISOR);
        stubContextLookups();

        BookingItem result = engine.apply(li, new Transition.Delegate(SUPERVISOR_ID, "Please review"), instructor());

        assertThat(result.getState()).isEqualTo(BookingState.AWAITING_SUPERVISOR);
        assertThat(result.getAssignedSupervisorUserId()).isEqualTo(SUPERVISOR_ID);

        assertThat(notifier.published()).hasSize(2);
        assertThat(notifier.published().get(0)).isInstanceOf(NotificationEvent.DelegatedToSupervisor.class);
        assertThat(notifier.published().get(1)).isInstanceOf(NotificationEvent.DelegatedAckToStudent.class);
        NotificationEvent.DelegatedToSupervisor sup = (NotificationEvent.DelegatedToSupervisor) notifier.published().get(0);
        assertThat(sup.supervisorUserId()).isEqualTo(SUPERVISOR_ID);
    }

    // ===== SupervisorApprove =====

    @Test
    void supervisorApprove_movesToSupervisorApproved_notifiesInstructor() {
        Booking b  = booking(BookingState.AWAITING_SUPERVISOR);
        BookingItem li = lineWithSupervisor(BookingState.AWAITING_SUPERVISOR);
        stubSave(b);
        stubRollup(BookingState.SUPERVISOR_APPROVED);
        stubContextLookups();

        BookingItem result = engine.apply(li, new Transition.SupervisorApprove("LGTM"), supervisor());

        assertThat(result.getState()).isEqualTo(BookingState.SUPERVISOR_APPROVED);
        assertThat(notifier.published()).hasSize(1);
        NotificationEvent.SupervisorApproved ev = (NotificationEvent.SupervisorApproved) notifier.published().get(0);
        assertThat(ev.instructorUserId()).isEqualTo(INSTRUCTOR_ID);
    }

    // ===== SupervisorDecline =====

    @Test
    void supervisorDecline_notifiesInstructorAndStudent() {
        Booking b  = booking(BookingState.AWAITING_SUPERVISOR);
        BookingItem li = lineWithSupervisor(BookingState.AWAITING_SUPERVISOR);
        stubSave(b);
        stubRollup(BookingState.SUPERVISOR_DECLINED);
        stubContextLookups();

        engine.apply(li, new Transition.SupervisorDecline("Unavailable"), supervisor());

        assertThat(notifier.published()).hasSize(2);
        assertThat(notifier.published().get(0)).isInstanceOf(NotificationEvent.SupervisorDeclinedToInstructor.class);
        assertThat(notifier.published().get(1)).isInstanceOf(NotificationEvent.SupervisorDeclinedToStudent.class);
    }

    // ===== Finalise =====

    @Test
    void finalise_fromSupervisorApproved_flipsItemAndNotifiesStudent() {
        Booking b  = booking(BookingState.SUPERVISOR_APPROVED);
        BookingItem li = line(BookingState.SUPERVISOR_APPROVED);
        LocalDateTime pickup = LocalDateTime.now().plusHours(2);
        stubSave(b);
        stubRollup(BookingState.READY_FOR_COLLECTION);
        stubContextLookups();
        when(itemClient.updateStatus(eq(ITEM_ID), any())).thenReturn(new ItemDto());

        BookingItem result = engine.apply(li, new Transition.Finalise(pickup, null), instructor());

        assertThat(result.getState()).isEqualTo(BookingState.READY_FOR_COLLECTION);
        verify(itemClient).updateStatus(eq(ITEM_ID), eq(Map.of("status", ItemStatus.IN_USE)));
        assertThat(notifier.published()).hasSize(1);
        assertThat(notifier.published().get(0)).isInstanceOf(NotificationEvent.ReadyForCollection.class);
    }

    // ===== MarkCollected =====

    @Test
    void markCollected_movesLineToCollected_noNotificationsNoFeign() {
        Booking b  = booking(BookingState.READY_FOR_COLLECTION);
        BookingItem li = line(BookingState.READY_FOR_COLLECTION);
        stubSave(b);
        stubRollup(BookingState.COLLECTED);

        BookingItem result = engine.apply(li, new Transition.MarkCollected(), instructor());

        assertThat(result.getState()).isEqualTo(BookingState.COLLECTED);
        assertThat(notifier.published()).isEmpty();
        verifyNoInteractions(userClient, itemClient, labClient);
    }

    // ===== MarkReturned =====

    @Test
    void markReturned_flipsItemToAvailable_noNotificationsNoFeign() {
        Booking b  = booking(BookingState.COLLECTED);
        BookingItem li = line(BookingState.COLLECTED);
        stubSave(b);
        stubRollup(BookingState.RETURNED);
        when(itemClient.updateStatus(eq(ITEM_ID), any())).thenReturn(new ItemDto());

        BookingItem result = engine.apply(li, new Transition.MarkReturned(), instructor());

        assertThat(result.getState()).isEqualTo(BookingState.RETURNED);
        verify(itemClient).updateStatus(eq(ITEM_ID), eq(Map.of("status", ItemStatus.AVAILABLE)));
        assertThat(notifier.published()).isEmpty();
        verifyNoInteractions(userClient, labClient);
    }

    // ===== FlipOverdue =====

    @Test
    void flipOverdue_notifiesStudentAndInstructor() {
        Booking b  = booking(BookingState.COLLECTED);
        BookingItem li = line(BookingState.COLLECTED);
        stubSave(b);
        stubRollup(BookingState.OVERDUE);
        stubContextLookups();

        engine.apply(li, new Transition.FlipOverdue(), null);

        assertThat(notifier.published()).hasSize(2);
        assertThat(notifier.published().get(0)).isInstanceOf(NotificationEvent.OverdueToStudent.class);
        assertThat(notifier.published().get(1)).isInstanceOf(NotificationEvent.OverdueToInstructor.class);
    }

    // ===== Cancel =====

    @Test
    void cancel_fromSubmitted_movesLineToCancelled_noItemFlip() {
        Booking b  = booking(BookingState.SUBMITTED);
        BookingItem li = line(BookingState.SUBMITTED);
        stubSave(b);
        stubRollup(BookingState.CANCELLED);

        BookingItem result = engine.apply(li, new Transition.Cancel(), student());

        assertThat(result.getState()).isEqualTo(BookingState.CANCELLED);
        verifyNoInteractions(itemClient, userClient, labClient);
    }

    @Test
    void cancel_fromReadyForCollection_flipsItemToAvailable() {
        Booking b  = booking(BookingState.READY_FOR_COLLECTION);
        BookingItem li = line(BookingState.READY_FOR_COLLECTION);
        stubSave(b);
        stubRollup(BookingState.CANCELLED);
        when(itemClient.updateStatus(eq(ITEM_ID), any())).thenReturn(new ItemDto());

        engine.apply(li, new Transition.Cancel(), student());

        verify(itemClient).updateStatus(eq(ITEM_ID), eq(Map.of("status", ItemStatus.AVAILABLE)));
    }

    // ===== BookingState.rollUp integration =====

    @Test
    void bookingStateIsRecomputedAfterTransition() {
        Booking b  = booking(BookingState.SUBMITTED);
        BookingItem li = line(BookingState.SUBMITTED);
        stubSave(b);
        stubRollup(BookingState.INSTRUCTOR_REVIEWING);

        engine.apply(li, new Transition.StartReview(), instructor());

        assertThat(b.getState()).isEqualTo(BookingState.INSTRUCTOR_REVIEWING);
        verify(bookingRepository).save(b);
    }
}
