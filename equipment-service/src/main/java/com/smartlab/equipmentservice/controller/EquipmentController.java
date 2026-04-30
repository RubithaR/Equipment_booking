package com.smartlab.equipmentservice.controller;

import com.smartlab.equipmentservice.dto.EquipmentRequest;
import com.smartlab.equipmentservice.dto.EquipmentResponse;
import com.smartlab.equipmentservice.service.EquipmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/equipment")
@RequiredArgsConstructor
public class EquipmentController {

    private final EquipmentService equipmentService;

    @PostMapping
    public ResponseEntity<EquipmentResponse> create(@Valid @RequestBody EquipmentRequest request) {
        return ResponseEntity.ok(equipmentService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<EquipmentResponse>> getAll() {
        return ResponseEntity.ok(equipmentService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EquipmentResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(equipmentService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EquipmentResponse> update(@PathVariable Long id, @Valid @RequestBody EquipmentRequest request) {
        return ResponseEntity.ok(equipmentService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<EquipmentResponse> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(equipmentService.updateStatus(id, body.get("status")));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        equipmentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<EquipmentResponse>> getByStatus(@PathVariable String status) {
        return ResponseEntity.ok(equipmentService.getByStatus(status));
    }
}
