package com.smartlab.bookingservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Multi-item booking request. The whole request shares one date window.
 * Each entry in {@code items} becomes a {@code booking_items} row with its
 * own approval lifecycle, routed to that item's lab's instructor.
 */
@Data
public class BookingRequest {

    @NotEmpty(message = "items must contain at least one entry")
    @Valid
    private List<Line> items;

    @NotBlank(message = "projectName is required")
    private String projectName;

    @NotBlank(message = "purpose is required")
    private String purpose;

    @NotNull(message = "startDate is required")
    private LocalDateTime startDate;

    @NotNull(message = "returnDate is required")
    private LocalDateTime returnDate;

    @NotNull(message = "studentDepartmentId is required")
    private Long studentDepartmentId;

    private Long nominatedSupervisorUserId;

    private List<AttachmentInput> attachments;

    @Data
    public static class Line {
        @NotNull(message = "itemId is required")
        private Long itemId;

        @NotNull(message = "labId is required")
        private Long labId;

        /** Optional lab-session time the student requests; meaningful for LAB_ONLY items. */
        private LocalDateTime requestedUseTime;
    }

    @Data
    public static class AttachmentInput {
        @NotBlank private String fileUrl;
        @NotBlank private String fileName;
        private String kind;
    }
}
