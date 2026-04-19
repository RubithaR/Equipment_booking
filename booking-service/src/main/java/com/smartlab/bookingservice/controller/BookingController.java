package com.smartlab.bookingservice.controller;

import com.smartlab.bookingservice.dto.BookingRequest;
import com.smartlab.bookingservice.dto.ReviewRequest;
import com.smartlab.bookingservice.entity.Booking;
import com.smartlab.bookingservice.service.BookingService;
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
    public ResponseEntity<Booking> create(@Valid @RequestBody BookingRequest request) {
        return ResponseEntity.ok(bookingService.createBooking(request));
    }

    @GetMapping
    public ResponseEntity<List<Booking>> getAll() {
        return ResponseEntity.ok(bookingService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Booking> getById(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.getById(id));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Booking>> getByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(bookingService.getByUserId(userId));
    }

    // Useful for instructor: GET /api/bookings/status/PENDING_APPROVAL
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Booking>> getByStatus(@PathVariable String status) {
        return ResponseEntity.ok(bookingService.getByStatus(status));
    }

    // Instructor approves a pending booking
    @PatchMapping("/{id}/approve")
    public ResponseEntity<Booking> approve(@PathVariable Long id,
                                           @Valid @RequestBody ReviewRequest review) {
        return ResponseEntity.ok(bookingService.approve(id, review));
    }

    // Instructor rejects a pending booking
    @PatchMapping("/{id}/reject")
    public ResponseEntity<Booking> reject(@PathVariable Long id,
                                          @Valid @RequestBody ReviewRequest review) {
        return ResponseEntity.ok(bookingService.reject(id, review));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Booking> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.cancelBooking(id));
    }
}
