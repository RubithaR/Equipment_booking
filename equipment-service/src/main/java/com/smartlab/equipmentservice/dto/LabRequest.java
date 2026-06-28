package com.smartlab.equipmentservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LabRequest {

    @NotNull(message = "departmentId is required")
    private Long departmentId;

    @NotBlank(message = "name is required")
    private String name;

    private String location;
    private String description;

    // Optional at create time; can be set later via PATCH /{id}/instructor
    private Long instructorUserId;
}
