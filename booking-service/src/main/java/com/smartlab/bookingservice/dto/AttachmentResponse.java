package com.smartlab.bookingservice.dto;

import com.smartlab.bookingservice.entity.BookingAttachment;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class AttachmentResponse {
    private Long id;
    private Long bookingId;
    private String fileUrl;
    private String fileName;
    private String kind;
    private Long uploadedByUserId;
    private Instant createdAt;

    public static AttachmentResponse from(BookingAttachment a) {
        return new AttachmentResponse(a.getId(), a.getBookingId(), a.getFileUrl(),
                a.getFileName(), a.getKind(), a.getUploadedByUserId(), a.getCreatedAt());
    }
}
