package com.smartlab.equipmentservice.controller;

import com.smartlab.equipmentservice.dto.LabRequest;
import com.smartlab.equipmentservice.dto.LabResponse;
import com.smartlab.equipmentservice.service.LabService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/labs")
@RequiredArgsConstructor
public class LabController {

    private final LabService labService;

    @PostMapping
    public ResponseEntity<LabResponse> create(@Valid @RequestBody LabRequest request) {
        return ResponseEntity.ok(labService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<LabResponse>> list(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long instructorUserId) {
        return ResponseEntity.ok(labService.list(departmentId, instructorUserId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LabResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(labService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LabResponse> update(@PathVariable Long id, @Valid @RequestBody LabRequest request) {
        return ResponseEntity.ok(labService.update(id, request));
    }

    @PatchMapping("/{id}/instructor")
    public ResponseEntity<LabResponse> assignInstructor(@PathVariable Long id,
                                                       @RequestBody Map<String, Long> body) {
        return ResponseEntity.ok(labService.assignInstructor(id, body.get("instructorUserId")));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        labService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
