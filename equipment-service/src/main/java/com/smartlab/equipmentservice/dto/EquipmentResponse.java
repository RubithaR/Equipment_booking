package com.smartlab.equipmentservice.dto;

import com.smartlab.equipmentservice.entity.Equipment;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentResponse {
    private Long id;
    private String name;
    private String category;
    private String location;
    private String status;
    private String description;

    // Lab info (resolved from labId)
    private Long labId;
    private String labName;
    // Instructor full names assigned to that lab (resolved via Feign to user-service)
    private List<String> instructorNames;

    public static EquipmentResponse fromEquipment(Equipment e) {
        EquipmentResponse r = new EquipmentResponse();
        r.setId(e.getId());
        r.setName(e.getName());
        r.setCategory(e.getCategory());
        r.setLocation(e.getLocation());
        r.setStatus(e.getStatus());
        r.setDescription(e.getDescription());
        r.setLabId(e.getLabId());
        return r;
    }
}
