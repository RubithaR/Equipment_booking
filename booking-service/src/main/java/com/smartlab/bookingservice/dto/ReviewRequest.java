package com.smartlab.bookingservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewRequest {
    @NotNull(message = "instructorId is required")
    private Long instructorId;

    private String note;
}
