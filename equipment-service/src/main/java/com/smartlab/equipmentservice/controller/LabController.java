package com.smartlab.equipmentservice.controller;

import com.smartlab.equipmentservice.dto.LabRequest;
import com.smartlab.equipmentservice.dto.LabResponse;
import com.smartlab.equipmentservice.service.LabService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<List<LabResponse>> getAll() {
        return ResponseEntity.ok(labService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LabResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(labService.getById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        labService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // Admin assigns an instructor to a lab
    @PostMapping("/{labId}/instructors/{instructorId}")
    public ResponseEntity<LabResponse> assignInstructor(@PathVariable Long labId,
                                                        @PathVariable Long instructorId) {
        return ResponseEntity.ok(labService.assignInstructor(labId, instructorId));
    }

    // Admin unassigns an instructor from a lab
    @DeleteMapping("/{labId}/instructors/{instructorId}")
    public ResponseEntity<Void> unassignInstructor(@PathVariable Long labId,
                                                   @PathVariable Long instructorId) {
        labService.unassignInstructor(labId, instructorId);
        return ResponseEntity.noContent().build();
    }

    // Instructor uses this to populate "which labs can I add equipment to"
    @GetMapping("/by-instructor/{instructorId}")
    public ResponseEntity<List<LabResponse>> getLabsForInstructor(@PathVariable Long instructorId) {
        return ResponseEntity.ok(labService.getLabsForInstructor(instructorId));
    }
}
