package com.smartlab.equipmentservice.dto;

import com.smartlab.equipmentservice.entity.Lab;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LabResponse {
    private Long id;
    private Long departmentId;
    private String name;
    private String location;
    private String description;
    private Long instructorUserId;

    public static LabResponse from(Lab l) {
        return new LabResponse(l.getId(), l.getDepartmentId(), l.getName(),
                l.getLocation(), l.getDescription(), l.getInstructorUserId());
    }
}
