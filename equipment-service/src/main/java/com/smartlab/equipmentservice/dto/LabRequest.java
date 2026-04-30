package com.smartlab.equipmentservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LabRequest {
    @NotBlank(message = "Lab name is required")
    private String name;
}
