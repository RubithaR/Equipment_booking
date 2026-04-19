package com.smartlab.bookingservice.dto;

import lombok.Data;

@Data
public class EquipmentDto {
    private Long id;
    private String name;
    private String category;
    private String location;
    private String status;
    private String description;
}
