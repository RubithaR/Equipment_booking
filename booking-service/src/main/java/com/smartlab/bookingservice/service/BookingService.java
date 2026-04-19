package com.smartlab.bookingservice.service;

import com.smartlab.bookingservice.client.EquipmentClient;
import com.smartlab.bookingservice.client.NotificationClient;
import com.smartlab.bookingservice.client.UserClient;
import com.smartlab.bookingservice.dto.BookingRequest;
import com.smartlab.bookingservice.dto.EquipmentDto;
import com.smartlab.bookingservice.dto.NotificationDto;
import com.smartlab.bookingservice.dto.ReviewRequest;
import com.smartlab.bookingservice.dto.UserDto;
import com.smartlab.bookingservice.entity.Booking;
import com.smartlab.bookingservice.exception.BadRequestException;
import com.smartlab.bookingservice.exception.ConflictException;
import com.smartlab.bookingservice.exception.NotFoundException;
import com.smartlab.bookingservice.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BookingService {

    public static final String STATUS_PENDING = "PENDING_APPROVAL";
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    private final BookingRepository bookingRepository;
    private final UserClient userClient;
    private final EquipmentClient equipmentClient;
    private final NotificationClient notificationClient;

    public Booking createBooking(BookingRequest request) {
        if (request.getStartTime() == null || request.getEndTime() == null) {
            throw new BadRequestException("startTime and endTime are required");
        }
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BadRequestException("endTime must be after startTime");
        }

        // Verify student exists
        UserDto student;
        try {
            student = userClient.getUserById(request.getUserId());
        } catch (Exception e) {
            throw new NotFoundException("User not found with id: " + request.getUserId());
        }

        // Verify equipment exists
        EquipmentDto equipment;
        try {
            equipment = equipmentClient.getEquipmentById(request.getEquipmentId());
        } catch (Exception e) {
            throw new NotFoundException("Equipment not found with id: " + request.getEquipmentId());
        }

        if ("MAINTENANCE".equalsIgnoreCase(equipment.getStatus())
                || "OUT_OF_SERVICE".equalsIgnoreCase(equipment.getStatus())) {
            throw new ConflictException("Equipment is not bookable. Current status: " + equipment.getStatus());
        }

        // Time-overlap check (against PENDING + CONFIRMED — both reserve the slot)
        List<Booking> conflicts = bookingRepository.findConflicts(
                request.getEquipmentId(), request.getStartTime(), request.getEndTime());
        if (!conflicts.isEmpty()) {
            Booking c = conflicts.get(0);
            throw new ConflictException(
                    "Equipment already booked from " + c.getStartTime() + " to " + c.getEndTime()
                    + " (booking #" + c.getId() + ", status " + c.getStatus() + ")");
        }

        Booking booking = new Booking();
        booking.setUserId(request.getUserId());
        booking.setEquipmentId(request.getEquipmentId());
        booking.setStartTime(request.getStartTime());
        booking.setEndTime(request.getEndTime());
        booking.setPurpose(request.getPurpose());
        booking.setStatus(STATUS_PENDING);
        booking.setCreatedAt(LocalDateTime.now());
        Booking saved = bookingRepository.save(booking);

        // Notify the student that their request was submitted
        sendNotification(saved.getUserId(),
                "Booking submitted",
                "Your booking #" + saved.getId() + " for " + equipment.getName()
                        + " is awaiting instructor approval.",
                "BOOKING_SUBMITTED");

        // Notify all instructors that a new booking needs review
        try {
            List<UserDto> instructors = userClient.getByRole("INSTRUCTOR");
            for (UserDto inst : instructors) {
                sendNotification(inst.getId(),
                        "New booking awaiting review",
                        student.getFullName() + " requested " + equipment.getName()
                                + " from " + saved.getStartTime() + " to " + saved.getEndTime() + ".",
                        "BOOKING_NEEDS_REVIEW");
            }
        } catch (Exception e) {
            System.err.println("Warning: failed to notify instructors: " + e.getMessage());
        }

        return saved;
    }

    public Booking approve(Long bookingId, ReviewRequest review) {
        Booking booking = getById(bookingId);
        if (!STATUS_PENDING.equals(booking.getStatus())) {
            throw new ConflictException("Only PENDING_APPROVAL bookings can be approved. Current: " + booking.getStatus());
        }
        booking.setStatus(STATUS_CONFIRMED);
        booking.setReviewedByInstructorId(review.getInstructorId());
        booking.setReviewedAt(LocalDateTime.now());
        booking.setReviewNote(review.getNote());
        Booking saved = bookingRepository.save(booking);

        // Mark equipment IN_USE only when approved
        try {
            equipmentClient.updateStatus(booking.getEquipmentId(), Map.of("status", "IN_USE"));
        } catch (Exception e) {
            System.err.println("Warning: failed to update equipment status: " + e.getMessage());
        }

        sendNotification(booking.getUserId(),
                "Booking approved",
                "Your booking #" + booking.getId() + " has been approved by instructor.",
                "BOOKING_APPROVED");
        return saved;
    }

    public Booking reject(Long bookingId, ReviewRequest review) {
        Booking booking = getById(bookingId);
        if (!STATUS_PENDING.equals(booking.getStatus())) {
            throw new ConflictException("Only PENDING_APPROVAL bookings can be rejected. Current: " + booking.getStatus());
        }
        booking.setStatus(STATUS_REJECTED);
        booking.setReviewedByInstructorId(review.getInstructorId());
        booking.setReviewedAt(LocalDateTime.now());
        booking.setReviewNote(review.getNote());
        Booking saved = bookingRepository.save(booking);

        sendNotification(booking.getUserId(),
                "Booking rejected",
                "Your booking #" + booking.getId() + " was rejected by instructor."
                        + (review.getNote() != null ? " Reason: " + review.getNote() : ""),
                "BOOKING_REJECTED");
        return saved;
    }

    public List<Booking> getAll() {
        return bookingRepository.findAll();
    }

    public Booking getById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Booking not found with id: " + id));
    }

    public List<Booking> getByUserId(Long userId) {
        return bookingRepository.findByUserId(userId);
    }

    public List<Booking> getByStatus(String status) {
        return bookingRepository.findByStatusOrderByCreatedAtDesc(status.toUpperCase());
    }

    public Booking cancelBooking(Long id) {
        Booking booking = getById(id);
        if (STATUS_CANCELLED.equals(booking.getStatus()) || STATUS_REJECTED.equals(booking.getStatus())) {
            throw new ConflictException("Booking is already " + booking.getStatus());
        }
        boolean wasConfirmed = STATUS_CONFIRMED.equals(booking.getStatus());
        booking.setStatus(STATUS_CANCELLED);
        Booking updated = bookingRepository.save(booking);

        // Only release equipment if it was actually marked IN_USE (i.e. previously CONFIRMED)
        if (wasConfirmed) {
            try {
                equipmentClient.updateStatus(booking.getEquipmentId(), Map.of("status", "AVAILABLE"));
            } catch (Exception e) {
                System.err.println("Warning: failed to release equipment: " + e.getMessage());
            }
        }

        sendNotification(booking.getUserId(),
                "Booking cancelled",
                "Your booking #" + booking.getId() + " has been cancelled.",
                "BOOKING_CANCELLED");
        return updated;
    }

    private void sendNotification(Long userId, String title, String message, String type) {
        try {
            notificationClient.send(new NotificationDto(userId, title, message, type));
        } catch (Exception e) {
            System.err.println("Warning: failed to send notification: " + e.getMessage());
        }
    }
}
