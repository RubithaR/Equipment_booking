package com.smartlab.bookingservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ItemDto {
    private Long id;
    private Long labId;
    private String model;
    private String name;
    private String category;
    private String serialNumber;
    private String status;
    private String description;
    private String conditionNote;
}
