package com.smartlab.equipmentservice.dto;

import com.smartlab.equipmentservice.entity.Item;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ItemResponse {
    private Long id;
    private Long labId;
    private String model;
    private String name;
    private String category;
    private String serialNumber;
    private String status;
    private String usageType;
    private String description;
    private String conditionNote;

    public static ItemResponse from(Item i) {
        return new ItemResponse(
                i.getId(), i.getLabId(), i.getModel(), i.getName(), i.getCategory(),
                i.getSerialNumber(), i.getStatus(), i.getUsageType(), i.getDescription(), i.getConditionNote());
    }
}
