package com.smartlab.bookingservice.service;

import com.smartlab.bookingservice.auth.BookingAuthorizer;
import com.smartlab.bookingservice.client.DepartmentClient;
import com.smartlab.bookingservice.client.ItemClient;
import com.smartlab.bookingservice.client.LabClient;
import com.smartlab.bookingservice.client.UserClient;
import com.smartlab.bookingservice.dto.BookingRequest;
import com.smartlab.bookingservice.dto.BookingResponse;
import com.smartlab.bookingservice.dto.DepartmentApprovalChain;
import com.smartlab.bookingservice.dto.ItemDto;
import com.smartlab.bookingservice.dto.LabDto;
import com.smartlab.bookingservice.dto.UserDto;
import com.smartlab.bookingservice.entity.Booking;
import com.smartlab.bookingservice.entity.BookingItem;
import com.smartlab.bookingservice.entity.BookingState;
import com.smartlab.bookingservice.notifier.NotificationEvent;
import com.smartlab.bookingservice.repository.BookingAttachmentRepository;
import com.smartlab.bookingservice.repository.BookingEventRepository;
import com.smartlab.bookingservice.repository.BookingItemRepository;
import com.smartlab.bookingservice.repository.BookingRepository;
import com.smartlab.bookingservice.transition.TransitionEngine;
import com.smartlab.notificationclient.InMemoryNotifier;
import com.smartlab.security.Roles;
import com.smartlab.security.UserContext;
import com.smartlab.security.exception.AuthorizationException;
import com.smartlab.security.exception.BadRequestException;
import com.smartlab.security.exception.ConflictException;
import com.smartlab.security.exception.NotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Coverage for the most complex method in booking-service — multi-line conflict
 * detection, item-status validation, lab-instructor existence, and notification
 * dispatch all run inside {@code create()}.
 */
@ExtendWith(MockitoExtension.class)
class BookingServiceCreateTest {

    @Mock BookingRepository           bookingRepository;
    @Mock BookingItemRepository       itemRepository;
    @Mock BookingEventRepository      eventRepository;
    @Mock BookingAttachmentRepository attachmentRepository;
    @Mock UserClient                  userClient;
    @Mock ItemClient                  itemClient;
    @Mock LabClient                   labClient;
    @Mock DepartmentClient            departmentClient;
    @Mock TransitionEngine            engine;

    InMemoryNotifier<NotificationEvent> notifier = new InMemoryNotifier<>();
    BookingAuthorizer authorizer = new BookingAuthorizer();
    BookingService service;

    static final Long STUDENT_ID    = 100L;
    static final Long INSTRUCTOR_ID = 200L;
    static final Long HOD_ID        = 300L;
    static final Long DEPT_ID       = 5L;
    static final Long ITEM_ID       = 20L;
    static final Long LAB_ID        = 30L;

    @BeforeEach
    void setUp() {
        service = new BookingService(bookingRepository, itemRepository, eventRepository,
                attachmentRepository, userClient, itemClient, labClient, departmentClient,
                notifier, engine, authorizer);
        notifier.clear();
        setActor(new UserContext(STUDENT_ID, "student@uni.com", Roles.STUDENT, null, DEPT_ID));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setActor(UserContext ctx) {
        var auth = new UsernamePasswordAuthenticationToken(ctx, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private BookingRequest validRequest() {
        BookingRequest req = new BookingRequest();
        BookingRequest.Line line = new BookingRequest.Line();
        line.setItemId(ITEM_ID);
        line.setLabId(LAB_ID);
        req.setItems(List.of(line));
        req.setProjectName("Project X");
        req.setPurpose("Research");
        req.setStartDate(LocalDateTime.now().plusDays(1));
        req.setReturnDate(LocalDateTime.now().plusDays(3));
        req.setStudentDepartmentId(DEPT_ID);
        return req;
    }

    private void stubHappyPath() {
        ItemDto item = new ItemDto();
        item.setId(ITEM_ID);
        item.setLabId(LAB_ID);
        item.setName("Oscilloscope");
        item.setStatus("AVAILABLE");

        LabDto lab = new LabDto();
        lab.setId(LAB_ID);
        lab.setName("Electronics Lab");
        lab.setInstructorUserId(INSTRUCTOR_ID);

        UserDto student = new UserDto();
        student.setId(STUDENT_ID);
        student.setFullName("Alice");

        UserDto hod = new UserDto();
        hod.setId(HOD_ID);
        hod.setFullName("Dr HoD");
        DepartmentApprovalChain chain = new DepartmentApprovalChain();
        chain.setHod(hod);

        lenient().when(itemClient.getItemById(ITEM_ID)).thenReturn(item);
        lenient().when(labClient.getLabById(LAB_ID)).thenReturn(lab);
        lenient().when(userClient.getUserById(STUDENT_ID)).thenReturn(student);
        lenient().when(departmentClient.getApprovalChain(DEPT_ID)).thenReturn(chain);
        lenient().when(itemRepository.findConflicts(any(), any(), any())).thenReturn(List.of());
        lenient().when(bookingRepository.save(any())).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(1L);
            return b;
        });
        lenient().when(itemRepository.save(any())).thenAnswer(inv -> {
            BookingItem li = inv.getArgument(0);
            li.setId(10L);
            return li;
        });
        lenient().when(eventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ===== Happy path =====

    @Test
    void create_validRequest_persistsAndRoutesToHod() {
        stubHappyPath();

        BookingResponse response = service.create(validRequest());

        assertThat(response).isNotNull();
        assertThat(response.getState()).isEqualTo(BookingState.AWAITING_HOD);
        assertThat(notifier.published()).hasSize(2);
        assertThat(notifier.published().get(0)).isInstanceOf(NotificationEvent.SubmittedAckToStudent.class);
        assertThat(notifier.published().get(1)).isInstanceOf(NotificationEvent.HodReviewNeeded.class);

        NotificationEvent.HodReviewNeeded review =
                (NotificationEvent.HodReviewNeeded) notifier.published().get(1);
        assertThat(review.hodUserId()).isEqualTo(HOD_ID);
        assertThat(review.studentFullName()).isEqualTo("Alice");
    }

    @Test
    void create_noHodConfigured_routesStraightToInstructor() {
        stubHappyPath();
        when(departmentClient.getApprovalChain(DEPT_ID)).thenReturn(new DepartmentApprovalChain());

        BookingResponse response = service.create(validRequest());

        assertThat(response.getState()).isEqualTo(BookingState.SUBMITTED);
        assertThat(notifier.published().get(1)).isInstanceOf(NotificationEvent.HodApprovedToInstructor.class);
        NotificationEvent.HodApprovedToInstructor toInstructor =
                (NotificationEvent.HodApprovedToInstructor) notifier.published().get(1);
        assertThat(toInstructor.instructorUserId()).isEqualTo(INSTRUCTOR_ID);
    }

    // ===== Authorization =====

    @Test
    void create_nonStudent_throws403() {
        setActor(new UserContext(STUDENT_ID, "i@uni.com", Roles.INSTRUCTOR, null, DEPT_ID));

        assertThatThrownBy(() -> service.create(validRequest()))
                .isInstanceOf(AuthorizationException.class)
                .hasMessageContaining("Only students");
    }

    // ===== Validation =====

    @Test
    void create_emptyItemList_throws400() {
        BookingRequest req = validRequest();
        req.setItems(List.of());

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("at least one item");
    }

    @Test
    void create_returnDateBeforeStart_throws400() {
        BookingRequest req = validRequest();
        LocalDateTime now = LocalDateTime.now();
        req.setStartDate(now.plusDays(3));
        req.setReturnDate(now.plusDays(1));

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("returnDate must be after");
    }

    @Test
    void create_duplicateItemInRequest_throws400() {
        BookingRequest req = validRequest();
        BookingRequest.Line dup = new BookingRequest.Line();
        dup.setItemId(ITEM_ID);
        dup.setLabId(LAB_ID);
        req.setItems(List.of(req.getItems().get(0), dup));

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Duplicate item");
    }

    @Test
    void create_itemLabMismatch_throws400() {
        ItemDto item = new ItemDto();
        item.setId(ITEM_ID);
        item.setLabId(99L);
        item.setStatus("AVAILABLE");
        when(itemClient.getItemById(ITEM_ID)).thenReturn(item);

        assertThatThrownBy(() -> service.create(validRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("does not belong to lab");
    }

    @Test
    void create_itemNotFound_throws404() {
        when(itemClient.getItemById(ITEM_ID)).thenThrow(new RuntimeException("404"));

        assertThatThrownBy(() -> service.create(validRequest()))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Item not found");
    }

    // ===== Item-status invariants =====

    @Test
    void create_maintenanceItem_throws409() {
        ItemDto item = new ItemDto();
        item.setId(ITEM_ID);
        item.setLabId(LAB_ID);
        item.setName("DAQ");
        item.setStatus("MAINTENANCE");
        when(itemClient.getItemById(ITEM_ID)).thenReturn(item);

        assertThatThrownBy(() -> service.create(validRequest()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("not bookable");
    }

    @Test
    void create_outOfServiceItem_throws409() {
        ItemDto item = new ItemDto();
        item.setId(ITEM_ID);
        item.setLabId(LAB_ID);
        item.setName("DAQ");
        item.setStatus("OUT_OF_SERVICE");
        when(itemClient.getItemById(ITEM_ID)).thenReturn(item);

        assertThatThrownBy(() -> service.create(validRequest()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("not bookable");
    }

    // ===== Lab invariants =====

    @Test
    void create_labWithoutInstructor_throws409() {
        ItemDto item = new ItemDto();
        item.setId(ITEM_ID);
        item.setLabId(LAB_ID);
        item.setName("Oscilloscope");
        item.setStatus("AVAILABLE");
        LabDto lab = new LabDto();
        lab.setId(LAB_ID);
        lab.setName("Electronics Lab");
        lab.setInstructorUserId(null);

        when(itemClient.getItemById(ITEM_ID)).thenReturn(item);
        when(labClient.getLabById(LAB_ID)).thenReturn(lab);

        assertThatThrownBy(() -> service.create(validRequest()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("no instructor assigned");
    }

    // ===== Conflict detection =====

    @Test
    void create_itemAlreadyBooked_throws409() {
        stubHappyPath();
        BookingItem clashing = new BookingItem();
        clashing.setBookingId(99L);
        clashing.setState(BookingState.SUBMITTED);
        when(itemRepository.findConflicts(any(), any(), any())).thenReturn(List.of(clashing));

        assertThatThrownBy(() -> service.create(validRequest()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already booked");
    }

    // ===== Booking persistence =====

    @Test
    void create_dispatchesAckToStudent() {
        stubHappyPath();

        service.create(validRequest());

        // The student always gets a submission acknowledgement, captured via the dispatched ack event.
        NotificationEvent.SubmittedAckToStudent ack =
                (NotificationEvent.SubmittedAckToStudent) notifier.published().get(0);
        assertThat(ack.studentUserId()).isEqualTo(STUDENT_ID);
        assertThat(ack.itemCount()).isEqualTo(1);
        assertThat(ack.labCount()).isEqualTo(1);
    }
}
