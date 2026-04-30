package com.smartlab.equipmentservice.dto;

import com.smartlab.equipmentservice.entity.Lab;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LabResponse {
    private Long id;
    private String name;
    // Instructor info (filled in by service via Feign call to user-service)
    private List<InstructorBrief> instructors;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InstructorBrief {
        private Long id;
        private String fullName;
        private String department;
    }

    public static LabResponse fromEntity(Lab lab, List<InstructorBrief> instructors) {
        return new LabResponse(lab.getId(), lab.getName(), instructors);
    }
}
