package com.smartlab.bookingservice.transition;

import com.smartlab.bookingservice.auth.BookingAuthorizer;
import com.smartlab.bookingservice.client.ItemClient;
import com.smartlab.bookingservice.client.LabClient;
import com.smartlab.bookingservice.client.UserClient;
import com.smartlab.bookingservice.dto.ItemDto;
import com.smartlab.bookingservice.dto.LabDto;
import com.smartlab.bookingservice.dto.UserDto;
import com.smartlab.bookingservice.entity.Booking;
import com.smartlab.bookingservice.entity.BookingEvent;
import com.smartlab.bookingservice.entity.BookingItem;
import com.smartlab.bookingservice.entity.BookingState;
import com.smartlab.security.exception.ConflictException;
import com.smartlab.security.exception.NotFoundException;
import com.smartlab.bookingservice.notifier.NotificationEvent;
import com.smartlab.notificationclient.Notifier;
import com.smartlab.bookingservice.repository.BookingEventRepository;
import com.smartlab.bookingservice.repository.BookingItemRepository;
import com.smartlab.bookingservice.repository.BookingRepository;
import com.smartlab.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Single skeleton for every Booking Item transition. Callers hand it a
 * {@link Transition} value; this method does the guard + mutate + persist +
 * event + item-status-flip + notifications + umbrella-state-recompute.
 *
 * The 9 specialised transition methods that used to live on BookingService
 * collapse to one call-site: {@code engine.apply(bookingId, lineId, transition, actor)}.
 */
@Component
@RequiredArgsConstructor
public class TransitionEngine {

    private static final Logger log = LoggerFactory.getLogger(TransitionEngine.class);

    private final BookingRepository bookingRepository;
    private final BookingItemRepository itemRepository;
    private final BookingEventRepository eventRepository;
    private final UserClient userClient;
    private final ItemClient itemClient;
    private final LabClient labClient;
    private final Notifier<NotificationEvent> notifier;
    private final BookingAuthorizer authorizer;

    @Transactional
    public BookingItem apply(Long bookingId, Long lineId, Transition t, UserContext actor) {
        BookingItem line = itemRepository.findById(lineId)
                .orElseThrow(() -> new NotFoundException("Booking item not found: " + lineId));
        if (!line.getBookingId().equals(bookingId)) {
            throw new ConflictException("Item " + lineId + " does not belong to booking " + bookingId);
        }
        return apply(line, t, actor);
    }

    /**
     * Apply a transition to an already-loaded line. Used by booking-level orchestrators
     * (cancel, scanOverdue) that loop over many lines and want to skip a fresh load per call.
     */
    @Transactional
    public BookingItem apply(BookingItem line, Transition t, UserContext actor) {
        Booking booking = bookingRepository.findById(line.getBookingId())
                .orElseThrow(() -> new NotFoundException("Booking not found: " + line.getBookingId()));

        if (!t.fromStates().contains(line.getState())) {
            throw new ConflictException(
                    line.getState() + " --> " + t.toState() + " is not a legal transition for line #" + line.getId());
        }
        authorizer.requireForTransition(actor, line, booking, t.requiredRole());

        String fromState = line.getState();
        t.mutate(line);
        line.setState(t.toState());
        line.setLastActorUserId(actor != null ? actor.userId() : null);
        BookingItem saved = itemRepository.save(line);

        recordEvent(saved, fromState, t.toState(), actor);
        t.itemStatusFlip(fromState).ifPresent(status -> flipItem(saved.getItemId(), status));
        recomputeBookingState(booking);

        if (t.hasNotifications()) {
            TransitionContext ctx = buildContext(booking, saved, fromState, actor);
            for (NotificationEvent ev : t.notifications(ctx)) {
                notifier.publish(ev);
            }
        }
        return saved;
    }

    /** Recompute aggregate booking state from the items and persist if it changed. */
    @Transactional
    public void recomputeBookingState(Booking booking) {
        List<BookingItem> lines = itemRepository.findByBookingIdOrderByIdAsc(booking.getId());
        String rolled = BookingState.rollUp(lines.stream().map(BookingItem::getState).collect(Collectors.toList()));
        if (!rolled.equals(booking.getState())) {
            String from = booking.getState();
            booking.setState(rolled);
            bookingRepository.save(booking);
            BookingEvent ev = new BookingEvent();
            ev.setBookingId(booking.getId());
            ev.setFromState(from);
            ev.setToState(rolled);
            ev.setNote("auto: aggregate state");
            eventRepository.save(ev);
        }
    }

    // ===== persistence helpers =====

    private void recordEvent(BookingItem saved, String fromState, String toState, UserContext actor) {
        BookingEvent ev = new BookingEvent();
        ev.setBookingId(saved.getBookingId());
        ev.setBookingItemId(saved.getId());
        ev.setActorUserId(actor != null ? actor.userId() : null);
        ev.setFromState(fromState);
        ev.setToState(toState);
        eventRepository.save(ev);
    }

    private void flipItem(Long itemId, String status) {
        try {
            itemClient.updateStatus(itemId, Map.of("status", status));
        } catch (Exception ex) {
            log.warn("Failed to flip item {} -> {}: {}", itemId, status, ex.getMessage());
        }
    }

    // ===== context assembly =====

    private TransitionContext buildContext(Booking booking, BookingItem line, String fromState, UserContext actor) {
        return new TransitionContext(
                booking, line, fromState, actor,
                safeUser(booking.getStudentUserId()),
                safeUser(line.getInstructorUserId()),
                line.getAssignedSupervisorUserId() != null
                        ? safeUser(line.getAssignedSupervisorUserId()) : null,
                safeItem(line.getItemId()),
                safeLab(line.getLabId()));
    }

    private UserDto safeUser(Long id) {
        if (id == null) return null;
        try { return userClient.getUserById(id); }
        catch (Exception ex) { log.warn("user lookup failed id={}: {}", id, ex.getMessage()); return null; }
    }

    private ItemDto safeItem(Long id) {
        if (id == null) return null;
        try { return itemClient.getItemById(id); }
        catch (Exception ex) { log.warn("item lookup failed id={}: {}", id, ex.getMessage()); return null; }
    }

    private LabDto safeLab(Long id) {
        if (id == null) return null;
        try { return labClient.getLabById(id); }
        catch (Exception ex) { log.warn("lab lookup failed id={}: {}", id, ex.getMessage()); return null; }
    }
}
