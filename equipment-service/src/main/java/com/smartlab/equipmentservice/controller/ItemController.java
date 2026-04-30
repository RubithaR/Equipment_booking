package com.smartlab.equipmentservice.controller;

import com.smartlab.equipmentservice.dto.ItemRequest;
import com.smartlab.equipmentservice.dto.ItemResponse;
import com.smartlab.equipmentservice.service.ItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @PostMapping
    public ResponseEntity<ItemResponse> create(@Valid @RequestBody ItemRequest request) {
        return ResponseEntity.ok(itemService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<ItemResponse>> list(
            @RequestParam(required = false) Long labId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String model) {
        return ResponseEntity.ok(itemService.list(labId, status, category, model));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ItemResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(itemService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ItemResponse> update(@PathVariable Long id, @Valid @RequestBody ItemRequest request) {
        return ResponseEntity.ok(itemService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ItemResponse> updateStatus(@PathVariable Long id,
                                                     @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(itemService.updateStatus(id, body.get("status")));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        itemService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
