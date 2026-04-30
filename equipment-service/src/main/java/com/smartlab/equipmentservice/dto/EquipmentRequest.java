package com.smartlab.equipmentservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EquipmentRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Category is required")
    private String category;

    @NotBlank(message = "Location is required")
    private String location;

    @NotBlank(message = "Status is required")
    private String status;

    private String description;

    // Required when an instructor creates equipment — must be one of their assigned labs
    private Long labId;

    // The instructor performing the create — server validates assignment to labId
    private Long instructorId;
}
