package com.smartlab.userservice.dto;

import com.smartlab.userservice.entity.Faculty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FacultyResponse {
    private Long id;
    private String code;
    private String name;

    public static FacultyResponse from(Faculty f) {
        return new FacultyResponse(f.getId(), f.getCode(), f.getName());
    }
}
