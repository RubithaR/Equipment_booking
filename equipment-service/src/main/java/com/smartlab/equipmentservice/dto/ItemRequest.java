package com.smartlab.equipmentservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ItemRequest {

    @NotNull(message = "labId is required")
    private Long labId;

    @NotBlank(message = "model is required")
    private String model;

    @NotBlank(message = "name is required")
    private String name;

    @NotBlank(message = "category is required")
    private String category;

    private String serialNumber;

    @NotBlank(message = "status is required")
    private String status;

    private String description;
    private String conditionNote;
}
