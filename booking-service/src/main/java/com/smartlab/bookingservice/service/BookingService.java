package com.smartlab.bookingservice.service;

import com.smartlab.bookingservice.auth.BookingAuthorizer;
import com.smartlab.security.Roles;
import com.smartlab.bookingservice.client.ItemClient;
import com.smartlab.bookingservice.client.LabClient;
import com.smartlab.bookingservice.client.UserClient;
import com.smartlab.bookingservice.dto.*;
import com.smartlab.bookingservice.entity.Booking;
import com.smartlab.bookingservice.entity.BookingAttachment;
import com.smartlab.bookingservice.entity.BookingEvent;
import com.smartlab.bookingservice.entity.BookingItem;
import com.smartlab.bookingservice.entity.BookingState;
import com.smartlab.security.ItemStatus;
import com.smartlab.security.exception.AuthorizationException;
import com.smartlab.security.exception.BadRequestException;
import com.smartlab.security.exception.ConflictException;
import com.smartlab.security.exception.NotFoundException;
import com.smartlab.bookingservice.notifier.NotificationEvent;
import com.smartlab.notificationclient.Notifier;
import com.smartlab.bookingservice.repository.BookingAttachmentRepository;
import com.smartlab.bookingservice.repository.BookingEventRepository;
import com.smartlab.bookingservice.repository.BookingItemRepository;
import com.smartlab.bookingservice.repository.BookingRepository;
import com.smartlab.security.CurrentUser;
import com.smartlab.security.UserContext;
import com.smartlab.bookingservice.transition.Transition;
import com.smartlab.bookingservice.transition.TransitionEngine;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Top-level coordinator for the Booking aggregate. Per-line state transitions
 * are delegated to {@link TransitionEngine}; this class handles creation,
 * multi-line orchestrations (cancel, overdue scan), and reads.
 */
@Service
@RequiredArgsConstructor
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookingRepository;
    private final BookingItemRepository itemRepository;
    private final BookingEventRepository eventRepository;
    private final BookingAttachmentRepository attachmentRepository;
    private final UserClient userClient;
    private final ItemClient itemClient;
    private final LabClient labClient;
    private final Notifier<NotificationEvent> notifier;
    private final TransitionEngine engine;
    private final BookingAuthorizer authorizer;

    // ===== Create =====

    @Transactional
    public BookingResponse create(BookingRequest request) {
        UserContext me = CurrentUser.require();
        if (!me.hasRole(Roles.STUDENT)) {
            throw new AuthorizationException("Only students can submit bookings");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BadRequestException("Booking must contain at least one item");
        }
        if (!request.getReturnDate().isAfter(request.getStartDate())) {
            throw new BadRequestException("returnDate must be after startDate");
        }

        Set<Long> seen = new HashSet<>();
        List<ResolvedLine> resolved = new ArrayList<>();
        for (BookingRequest.Line line : request.getItems()) {
            if (line.getItemId() == null || line.getLabId() == null) {
                throw new BadRequestException("Every item line must include itemId and labId");
            }
            if (!seen.add(line.getItemId())) {
                throw new BadRequestException("Duplicate item in request: #" + line.getItemId());
            }

            ItemDto item;
            try { item = itemClient.getItemById(line.getItemId()); }
            catch (Exception e) { throw new NotFoundException("Item not found: " + line.getItemId()); }
            if (!item.getLabId().equals(line.getLabId())) {
                throw new BadRequestException("Item #" + item.getId() + " does not belong to lab #" + line.getLabId());
            }
            if (ItemStatus.blocksBooking(item.getStatus())) {
                throw new ConflictException("Item " + item.getName() + " is not bookable (status "
                        + item.getStatus() + ")");
            }

            LabDto lab;
            try { lab = labClient.getLabById(line.getLabId()); }
            catch (Exception e) { throw new NotFoundException("Lab not found: " + line.getLabId()); }
            if (lab.getInstructorUserId() == null) {
                throw new ConflictException("Lab " + lab.getName() + " has no instructor assigned yet — please pick another item.");
            }

            List<BookingItem> conflicts = itemRepository.findConflicts(
                    item.getId(), request.getStartDate(), request.getReturnDate());
            if (!conflicts.isEmpty()) {
                BookingItem c = conflicts.get(0);
                throw new ConflictException(
                        "Item " + item.getName() + " is already booked in window "
                                + " (booking #" + c.getBookingId() + ", state " + c.getState() + ")");
            }

            resolved.add(new ResolvedLine(item, lab, line.getRequestedUseTime()));
        }

        Booking b = new Booking();
        b.setStudentUserId(me.userId());
        b.setStudentDepartmentId(request.getStudentDepartmentId());
        b.setProjectName(request.getProjectName());
        b.setPurpose(request.getPurpose());
        b.setStartDate(request.getStartDate());
        b.setReturnDate(request.getReturnDate());
        b.setNominatedSupervisorUserId(request.getNominatedSupervisorUserId());
        b.setState(BookingState.SUBMITTED);
        Booking savedBooking = bookingRepository.save(b);

        List<BookingItem> savedLines = new ArrayList<>();
        for (ResolvedLine r : resolved) {
            BookingItem bi = new BookingItem();
            bi.setBookingId(savedBooking.getId());
            bi.setItemId(r.item.getId());
            bi.setLabId(r.lab.getId());
            bi.setInstructorUserId(r.lab.getInstructorUserId());
            // Snapshot the item's usage type (borrowable / lab-only) at submission.
            bi.setUsageType(r.item.getUsageType() != null ? r.item.getUsageType() : "BORROWABLE");
            bi.setRequestedUseTime(r.requestedUseTime());
            bi.setState(BookingState.SUBMITTED);
            bi.setLastActorUserId(me.userId());
            BookingItem saved = itemRepository.save(bi);
            savedLines.add(saved);
            BookingEvent ev = new BookingEvent();
            ev.setBookingId(savedBooking.getId());
            ev.setBookingItemId(saved.getId());
            ev.setActorUserId(me.userId());
            ev.setToState(BookingState.SUBMITTED);
            eventRepository.save(ev);
        }
        BookingEvent umbrellaEv = new BookingEvent();
        umbrellaEv.setBookingId(savedBooking.getId());
        umbrellaEv.setActorUserId(me.userId());
        umbrellaEv.setToState(BookingState.SUBMITTED);
        umbrellaEv.setNote("Submitted with " + savedLines.size() + " item(s)");
        eventRepository.save(umbrellaEv);

        if (request.getAttachments() != null) {
            for (BookingRequest.AttachmentInput in : request.getAttachments()) {
                BookingAttachment a = new BookingAttachment();
                a.setBookingId(savedBooking.getId());
                a.setFileUrl(in.getFileUrl());
                a.setFileName(in.getFileName());
                a.setKind(in.getKind() == null ? "OTHER" : in.getKind());
                a.setUploadedByUserId(me.userId());
                attachmentRepository.save(a);
            }
        }

        // The student's department HOD reviews this from their queue and assigns a handler.
        Set<Long> labIds = resolved.stream().map(r -> r.lab.getId()).collect(Collectors.toSet());
        notifier.publish(new NotificationEvent.SubmittedAckToStudent(
                savedBooking.getId(), me.userId(), savedLines.size(), labIds.size()));

        return BookingResponse.from(savedBooking, savedLines);
    }

    // ===== Per-line transitions (umbrella endpoint) =====

    @Transactional
    public BookingResponse applyTransition(Long bookingId, Long lineId, Transition transition) {
        UserContext me = CurrentUser.require();
        engine.apply(bookingId, lineId, transition, me);
        return loadResponse(bookingId);
    }

    // ===== Booking-level student action =====

    @Transactional
    public BookingResponse cancel(Long bookingId) {
        UserContext me = CurrentUser.require();
        Booking booking = getOrThrow(bookingId);
        authorizer.requireCanCancel(me, booking);
        List<BookingItem> lines = itemRepository.findByBookingIdOrderByIdAsc(bookingId);
        Set<Long> notifyTargets = new java.util.LinkedHashSet<>();
        Transition.Cancel cancel = new Transition.Cancel();
        int cancelled = 0;
        for (BookingItem line : lines) {
            if (!BookingState.isCancellable(line.getState())) continue;
            engine.apply(line, cancel, me);
            notifyTargets.add(line.getInstructorUserId());
            cancelled++;
        }
        if (cancelled == 0) {
            throw new ConflictException("Nothing left to cancel — every line is already collected or terminal.");
        }
        for (Long instructorId : notifyTargets) {
            notifier.publish(new NotificationEvent.BookingCancelled(bookingId, instructorId, cancelled));
        }
        return loadResponse(bookingId);
    }

    // ===== Cron entry point =====

    @Transactional
    public int scanOverdue() {
        LocalDateTime now = LocalDateTime.now();
        List<BookingItem> due = itemRepository.findCollectedPastDue(now);
        if (due.isEmpty()) return 0;
        Transition.FlipOverdue flip = new Transition.FlipOverdue();
        for (BookingItem line : due) {
            engine.apply(line, flip, null);
        }
        log.info("scanOverdue.flipped count={}", due.size());
        return due.size();
    }

    // ===== Reads =====

    public BookingResponse getById(Long id) {
        Booking b = getOrThrow(id);
        List<BookingItem> items = itemRepository.findByBookingIdOrderByIdAsc(id);
        authorizer.requireCanRead(CurrentUser.require(), b, items);
        return BookingResponse.from(b, items);
    }

    public List<BookingResponse> listForCurrentStudent() {
        UserContext me = CurrentUser.require();
        return hydrate(bookingRepository.findByStudentUserIdOrderByCreatedAtDesc(me.userId()));
    }

    /** Handler queue — lines an HOD has assigned to me (instructor / lecturer / HOD). */
    public List<BookingResponse> listForCurrentInstructor() {
        UserContext me = CurrentUser.require();
        if (!me.hasAnyRole(Roles.INSTRUCTOR, Roles.LECTURER, Roles.HOD)) {
            throw new AuthorizationException("Only staff can view this list");
        }
        // Exclude SUBMITTED — those are still awaiting HOD review, not yet handed to a handler.
        List<BookingItem> myLines = itemRepository.findByInstructorUserIdOrderByCreatedAtDesc(me.userId()).stream()
                .filter(l -> !BookingState.SUBMITTED.equals(l.getState()))
                .collect(Collectors.toList());
        Set<Long> bookingIds = myLines.stream().map(BookingItem::getBookingId).collect(Collectors.toSet());
        return hydrate(bookingRepository.findAllById(bookingIds));
    }

    /** HOD dashboard Tab 1 — booking requests from my department awaiting my review. */
    public List<BookingResponse> listAwaitingHod() {
        UserContext me = CurrentUser.require();
        if (!me.hasRole(Roles.HOD)) {
            throw new AuthorizationException("Only HODs can view this list");
        }
        Long dept = me.departmentId();
        if (dept == null) return List.of();
        return hydrate(bookingRepository
                .findByStudentDepartmentIdAndStateOrderByCreatedAtDesc(dept, BookingState.SUBMITTED));
    }

    /** HOD dashboard Tab 2 — requests from my department I've already processed (approved/sent or rejected). */
    public List<BookingResponse> listHodProcessed() {
        UserContext me = CurrentUser.require();
        if (!me.hasRole(Roles.HOD)) {
            throw new AuthorizationException("Only HODs can view this list");
        }
        Long dept = me.departmentId();
        if (dept == null) return List.of();
        List<Booking> rows = bookingRepository.findByStudentDepartmentIdOrderByCreatedAtDesc(dept).stream()
                .filter(b -> !BookingState.SUBMITTED.equals(b.getState()))
                .collect(Collectors.toList());
        return hydrate(rows);
    }

    public List<BookingResponse> listAll(String state) {
        UserContext me = CurrentUser.require();
        authorizer.requireAdminListAccess(me);
        Long deptId = authorizer.listScopeDepartmentId(me).orElse(null);
        List<Booking> rows;
        if (state == null && deptId == null)         rows = bookingRepository.findAll();
        else if (state == null)                      rows = bookingRepository.findByStudentDepartmentIdOrderByCreatedAtDesc(deptId);
        else if (deptId == null)                     rows = bookingRepository.findByStateOrderByCreatedAtDesc(state);
        else                                         rows = bookingRepository.findByStudentDepartmentIdAndStateOrderByCreatedAtDesc(deptId, state);
        return hydrate(rows);
    }

    /**
     * Booking-derived availability for a set of items — what the student catalogue
     * and cart use to show "in use" and the date each item is free again. Any active
     * hold (awaiting approval or approved) reports as IN_USE; items with no hold come
     * back as AVAILABLE so the caller can render every requested id in one pass.
     */
    public List<ItemAvailabilityResponse> availability(List<Long> itemIds) {
        CurrentUser.require();
        if (itemIds == null || itemIds.isEmpty()) return List.of();

        Map<Long, List<ActiveWindow>> byItem = itemRepository.findActiveWindows(itemIds).stream()
                .collect(Collectors.groupingBy(ActiveWindow::itemId));

        List<ItemAvailabilityResponse> out = new ArrayList<>();
        for (Long itemId : itemIds) {
            List<ActiveWindow> holds = byItem.get(itemId);
            if (holds == null || holds.isEmpty()) {
                out.add(new ItemAvailabilityResponse(itemId, "AVAILABLE", null, List.of()));
                continue;
            }
            LocalDateTime bookedUntil = holds.stream()
                    .map(ActiveWindow::end).max(LocalDateTime::compareTo).orElse(null);
            List<ItemAvailabilityResponse.Window> windows = holds.stream()
                    .map(w -> new ItemAvailabilityResponse.Window(
                            w.start(), w.end(), w.state(), BookingState.availabilityBucket(w.state())))
                    .collect(Collectors.toList());
            // Any active hold — awaiting approval or already approved — shows to students as "In use".
            out.add(new ItemAvailabilityResponse(itemId, "IN_USE", bookedUntil, windows));
        }
        return out;
    }

    public List<EventResponse> timeline(Long bookingId) {
        Booking b = getOrThrow(bookingId);
        List<BookingItem> items = itemRepository.findByBookingIdOrderByIdAsc(bookingId);
        authorizer.requireCanRead(CurrentUser.require(), b, items);
        return eventRepository.findByBookingIdOrderByCreatedAtAsc(bookingId).stream()
                .map(EventResponse::from).collect(Collectors.toList());
    }

    public List<AttachmentResponse> attachments(Long bookingId) {
        Booking b = getOrThrow(bookingId);
        List<BookingItem> items = itemRepository.findByBookingIdOrderByIdAsc(bookingId);
        authorizer.requireCanRead(CurrentUser.require(), b, items);
        return attachmentRepository.findByBookingIdOrderByCreatedAtAsc(bookingId).stream()
                .map(AttachmentResponse::from).collect(Collectors.toList());
    }

    // ===== helpers =====

    private List<BookingResponse> hydrate(List<Booking> bookings) {
        if (bookings.isEmpty()) return List.of();
        List<Long> ids = bookings.stream().map(Booking::getId).toList();
        Map<Long, List<BookingItem>> byBooking = itemRepository
                .findByBookingIdInOrderByBookingIdAscIdAsc(ids).stream()
                .collect(Collectors.groupingBy(BookingItem::getBookingId));
        return bookings.stream()
                .map(b -> BookingResponse.from(b, byBooking.getOrDefault(b.getId(), List.of())))
                .toList();
    }

    private BookingResponse loadResponse(Long bookingId) {
        Booking b = getOrThrow(bookingId);
        List<BookingItem> items = itemRepository.findByBookingIdOrderByIdAsc(bookingId);
        return BookingResponse.from(b, items);
    }

    private Booking getOrThrow(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Booking not found: " + id));
    }

    private UserDto safeGetUser(Long id) {
        try { return userClient.getUserById(id); }
        catch (Exception ex) { log.warn("user lookup failed id={}", id, ex); return null; }
    }

    private record ResolvedLine(ItemDto item, LabDto lab, java.time.LocalDateTime requestedUseTime) {}
}
