package com.smartlab.userservice.dto;

import com.smartlab.userservice.entity.Department;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DepartmentResponse {
    private Long id;
    private Long facultyId;
    private String code;
    private String name;
    private Long hodUserId;

    public static DepartmentResponse from(Department d) {
        return new DepartmentResponse(d.getId(), d.getFacultyId(), d.getCode(), d.getName(), d.getHodUserId());
    }
}
