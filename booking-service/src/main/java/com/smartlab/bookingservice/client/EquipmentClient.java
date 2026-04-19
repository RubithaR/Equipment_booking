package com.smartlab.bookingservice.client;

import com.smartlab.bookingservice.dto.EquipmentDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "equipment-service")
public interface EquipmentClient {

    @GetMapping("/api/equipment/{id}")
    EquipmentDto getEquipmentById(@PathVariable("id") Long id);

    @PatchMapping("/api/equipment/{id}/status")
    EquipmentDto updateStatus(@PathVariable("id") Long id, @RequestBody Map<String, String> body);
}
