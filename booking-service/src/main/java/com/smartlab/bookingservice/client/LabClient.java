package com.smartlab.bookingservice.client;

import com.smartlab.bookingservice.dto.LabDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "equipment-service", contextId = "labClient")
public interface LabClient {
    @GetMapping("/api/labs/{id}")
    LabDto getLabById(@PathVariable("id") Long id);
}
