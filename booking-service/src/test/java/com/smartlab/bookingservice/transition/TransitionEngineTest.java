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
    static final Long HANDLER_ID    = 200L;   // assigned handler (instructor/lecturer/HOD)
    static final Long HOD_ID        = 400L;   // the student's department HOD
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
        li.setInstructorUserId(HANDLER_ID);   // lab default at submit; the assigned handler thereafter
        li.setState(state);
        return li;
    }

    private UserContext hod() {
        return new UserContext(HOD_ID, "hod@uni.com", "HOD", null, DEPT_ID);
    }

    private UserContext handler() {
        return new UserContext(HANDLER_ID, "handler@lab.com", "INSTRUCTOR", null, DEPT_ID);
    }

    private UserContext student() {
        return new UserContext(STUDENT_ID, "student@uni.com", "STUDENT", null, DEPT_ID);
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
        UserDto studentU = new UserDto(); studentU.setFullName("Alice");  studentU.setId(STUDENT_ID);
        UserDto handlerU = new UserDto(); handlerU.setFullName("Bob");    handlerU.setId(HANDLER_ID);
        ItemDto item = new ItemDto(); item.setId(ITEM_ID); item.setName("Oscilloscope"); item.setLabId(LAB_ID);
        LabDto  lab  = new LabDto();  lab.setId(LAB_ID);  lab.setName("Electronics Lab");

        when(userClient.getUserById(STUDENT_ID)).thenReturn(studentU);
        when(userClient.getUserById(HANDLER_ID)).thenReturn(handlerU);
        when(itemClient.getItemById(ITEM_ID)).thenReturn(item);
        when(labClient.getLabById(LAB_ID)).thenReturn(lab);
    }

    // ===== HodApprove =====

    @Test
    void hodApprove_assignsHandler_movesToAwaitingHandler_notifiesHandlerAndStudent() {
        Booking b = booking(BookingState.SUBMITTED);
        BookingItem li = line(BookingState.SUBMITTED);
        stubSave(b);
        stubRollup(BookingState.AWAITING_HANDLER);
        stubContextLookups();

        BookingItem result = engine.apply(li, new Transition.HodApprove(HANDLER_ID, "Please handle"), hod());

        assertThat(result.getState()).isEqualTo(BookingState.AWAITING_HANDLER);
        assertThat(result.getInstructorUserId()).isEqualTo(HANDLER_ID);
        assertThat(notifier.published()).hasSize(2);
        assertThat(notifier.published().get(0)).isInstanceOf(NotificationEvent.DelegatedToSupervisor.class);
        assertThat(notifier.published().get(1)).isInstanceOf(NotificationEvent.DelegatedAckToStudent.class);
        NotificationEvent.DelegatedToSupervisor toHandler =
                (NotificationEvent.DelegatedToSupervisor) notifier.published().get(0);
        assertThat(toHandler.supervisorUserId()).isEqualTo(HANDLER_ID);
    }

    // ===== HodReject =====

    @Test
    void hodReject_movesLineToRejected_notifiesStudent() {
        Booking b = booking(BookingState.SUBMITTED);
        BookingItem li = line(BookingState.SUBMITTED);
        stubSave(b);
        stubRollup(BookingState.REJECTED);
        stubContextLookups();

        BookingItem result = engine.apply(li, new Transition.HodReject("Out of scope"), hod());

        assertThat(result.getState()).isEqualTo(BookingState.REJECTED);
        assertThat(notifier.published()).hasSize(1);
        NotificationEvent.ItemRejected ev = (NotificationEvent.ItemRejected) notifier.published().get(0);
        assertThat(ev.studentUserId()).isEqualTo(STUDENT_ID);
        assertThat(ev.reason()).isEqualTo("Out of scope");
    }

    // ===== HandlerApprove =====

    @Test
    void handlerApprove_fromAwaitingHandler_setsPickupFlipsItemNotifiesStudent() {
        Booking b = booking(BookingState.AWAITING_HANDLER);
        BookingItem li = line(BookingState.AWAITING_HANDLER);
        LocalDateTime pickup = LocalDateTime.now().plusDays(1);
        stubSave(b);
        stubRollup(BookingState.READY_FOR_COLLECTION);
        stubContextLookups();
        when(itemClient.updateStatus(eq(ITEM_ID), any())).thenReturn(new ItemDto());

        BookingItem result = engine.apply(li, new Transition.HandlerApprove(pickup, "Bring your ID"), handler());

        assertThat(result.getState()).isEqualTo(BookingState.READY_FOR_COLLECTION);
        assertThat(result.getPickupAt()).isEqualTo(pickup);
        assertThat(result.getPickupNote()).isEqualTo("Bring your ID");
        verify(itemClient).updateStatus(eq(ITEM_ID), eq(Map.of("status", ItemStatus.IN_USE)));
        assertThat(notifier.published()).hasSize(1);
        assertThat(notifier.published().get(0)).isInstanceOf(NotificationEvent.ReadyForCollection.class);
        NotificationEvent.ReadyForCollection ev = (NotificationEvent.ReadyForCollection) notifier.published().get(0);
        assertThat(ev.studentUserId()).isEqualTo(STUDENT_ID);
    }

    // ===== HandlerReject =====

    @Test
    void handlerReject_movesLineToRejected_notifiesStudent() {
        Booking b = booking(BookingState.AWAITING_HANDLER);
        BookingItem li = line(BookingState.AWAITING_HANDLER);
        stubSave(b);
        stubRollup(BookingState.REJECTED);
        stubContextLookups();

        BookingItem result = engine.apply(li, new Transition.HandlerReject("Item busy"), handler());

        assertThat(result.getState()).isEqualTo(BookingState.REJECTED);
        assertThat(notifier.published()).hasSize(1);
        assertThat(notifier.published().get(0)).isInstanceOf(NotificationEvent.ItemRejected.class);
    }

    // ===== MarkCollected =====

    @Test
    void markCollected_movesLineToCollected_noNotificationsNoFeign() {
        Booking b = booking(BookingState.READY_FOR_COLLECTION);
        BookingItem li = line(BookingState.READY_FOR_COLLECTION);
        stubSave(b);
        stubRollup(BookingState.COLLECTED);

        BookingItem result = engine.apply(li, new Transition.MarkCollected(), handler());

        assertThat(result.getState()).isEqualTo(BookingState.COLLECTED);
        assertThat(notifier.published()).isEmpty();
        verifyNoInteractions(userClient, itemClient, labClient);
    }

    // ===== MarkReturned =====

    @Test
    void markReturned_flipsItemToAvailable_noNotificationsNoFeign() {
        Booking b = booking(BookingState.COLLECTED);
        BookingItem li = line(BookingState.COLLECTED);
        stubSave(b);
        stubRollup(BookingState.RETURNED);
        when(itemClient.updateStatus(eq(ITEM_ID), any())).thenReturn(new ItemDto());

        BookingItem result = engine.apply(li, new Transition.MarkReturned(), handler());

        assertThat(result.getState()).isEqualTo(BookingState.RETURNED);
        verify(itemClient).updateStatus(eq(ITEM_ID), eq(Map.of("status", ItemStatus.AVAILABLE)));
        assertThat(notifier.published()).isEmpty();
        verifyNoInteractions(userClient, labClient);
    }

    // ===== FlipOverdue =====

    @Test
    void flipOverdue_notifiesStudentAndHandler() {
        Booking b = booking(BookingState.COLLECTED);
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
        Booking b = booking(BookingState.SUBMITTED);
        BookingItem li = line(BookingState.SUBMITTED);
        stubSave(b);
        stubRollup(BookingState.CANCELLED);

        BookingItem result = engine.apply(li, new Transition.Cancel(), student());

        assertThat(result.getState()).isEqualTo(BookingState.CANCELLED);
        verifyNoInteractions(itemClient, userClient, labClient);
    }

    @Test
    void cancel_fromReadyForCollection_flipsItemToAvailable() {
        Booking b = booking(BookingState.READY_FOR_COLLECTION);
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
        Booking b = booking(BookingState.SUBMITTED);
        BookingItem li = line(BookingState.SUBMITTED);
        stubSave(b);
        stubRollup(BookingState.AWAITING_HANDLER);
        stubContextLookups();

        engine.apply(li, new Transition.HodApprove(HANDLER_ID, null), hod());

        assertThat(b.getState()).isEqualTo(BookingState.AWAITING_HANDLER);
        verify(bookingRepository).save(b);
    }
}
