package com.smartlab.bookingservice.controller;

import com.smartlab.bookingservice.dto.AttachmentResponse;
import com.smartlab.bookingservice.dto.BookingRequest;
import com.smartlab.bookingservice.dto.BookingResponse;
import com.smartlab.bookingservice.dto.EventResponse;
import com.smartlab.bookingservice.dto.ItemAvailabilityResponse;
import com.smartlab.bookingservice.service.BookingService;
import com.smartlab.bookingservice.transition.Transition;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingResponse> create(@Valid @RequestBody BookingRequest request) {
        return ResponseEntity.ok(bookingService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.getById(id));
    }

    @GetMapping("/{id}/timeline")
    public ResponseEntity<List<EventResponse>> timeline(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.timeline(id));
    }

    @GetMapping("/{id}/attachments")
    public ResponseEntity<List<AttachmentResponse>> attachments(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.attachments(id));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<BookingResponse>> mine() {
        return ResponseEntity.ok(bookingService.listForCurrentStudent());
    }

    @GetMapping("/assigned-to-me")
    public ResponseEntity<List<BookingResponse>> assignedToMe() {
        return ResponseEntity.ok(bookingService.listForCurrentInstructor());
    }

    @GetMapping("/awaiting-my-supervision")
    public ResponseEntity<List<BookingResponse>> awaitingMySupervision() {
        return ResponseEntity.ok(bookingService.listForCurrentSupervisor());
    }

    @GetMapping
    public ResponseEntity<List<BookingResponse>> listAll(@RequestParam(required = false) String state) {
        return ResponseEntity.ok(bookingService.listAll(state));
    }

    /**
     * Booking-derived availability for the given items — drives the student
     * catalogue's "In process" / "In use" badges and the cart's date-overlap
     * warnings. Call with {@code ?itemIds=1,2,3}.
     */
    @GetMapping("/availability")
    public ResponseEntity<List<ItemAvailabilityResponse>> availability(@RequestParam List<Long> itemIds) {
        return ResponseEntity.ok(bookingService.availability(itemIds));
    }

    /**
     * Single umbrella endpoint for every per-line state transition. Body is a
     * polymorphic {@link Transition}: {@code { "type": "APPROVE_DIRECTLY", ... }}.
     * Replaces the 9 endpoints that previously mirrored each transition by name.
     */
    @PostMapping("/{id}/items/{itemId}/transition")
    public ResponseEntity<BookingResponse> transition(@PathVariable Long id,
                                                      @PathVariable Long itemId,
                                                      @Valid @RequestBody Transition body) {
        return ResponseEntity.ok(bookingService.applyTransition(id, itemId, body));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<BookingResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.cancel(id));
    }
}
