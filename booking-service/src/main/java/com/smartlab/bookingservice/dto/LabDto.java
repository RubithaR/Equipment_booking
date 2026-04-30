package com.smartlab.bookingservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LabDto {
    private Long id;
    private Long departmentId;
    private String name;
    private String location;
    private String description;
    private Long instructorUserId;
}
