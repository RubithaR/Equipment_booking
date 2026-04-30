package com.smartlab.bookingservice.dto;

import com.smartlab.bookingservice.entity.Booking;
import com.smartlab.bookingservice.entity.BookingItem;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class BookingResponse {
    private Long id;
    private Long studentUserId;
    private Long studentDepartmentId;
    private String projectName;
    private String purpose;
    private LocalDateTime startDate;
    private LocalDateTime returnDate;
    private Long nominatedSupervisorUserId;
    private String state;
    private Instant createdAt;
    private Instant updatedAt;
    private List<BookingItemResponse> items;

    public static BookingResponse from(Booking b, List<BookingItem> items) {
        return new BookingResponse(
                b.getId(), b.getStudentUserId(), b.getStudentDepartmentId(),
                b.getProjectName(), b.getPurpose(),
                b.getStartDate(), b.getReturnDate(),
                b.getNominatedSupervisorUserId(),
                b.getState(),
                b.getCreatedAt(), b.getUpdatedAt(),
                items.stream().map(BookingItemResponse::from).toList());
    }
}
