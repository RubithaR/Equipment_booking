package com.smartlab.bookingservice.client;

import com.smartlab.bookingservice.dto.ItemDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "equipment-service", contextId = "itemClient")
public interface ItemClient {

    @GetMapping("/api/items/{id}")
    ItemDto getItemById(@PathVariable("id") Long id);

    @PatchMapping("/api/items/{id}/status")
    ItemDto updateStatus(@PathVariable("id") Long id, @RequestBody Map<String, String> body);
}
