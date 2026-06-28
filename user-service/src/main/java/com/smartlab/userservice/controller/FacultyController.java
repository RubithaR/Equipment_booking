package com.smartlab.userservice.controller;

import com.smartlab.userservice.dto.FacultyResponse;
import com.smartlab.security.exception.NotFoundException;
import com.smartlab.userservice.repository.FacultyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/faculties")
@RequiredArgsConstructor
public class FacultyController {

    private final FacultyRepository facultyRepository;

    @GetMapping
    public ResponseEntity<List<FacultyResponse>> list() {
        return ResponseEntity.ok(
                facultyRepository.findAll().stream().map(FacultyResponse::from).collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FacultyResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(FacultyResponse.from(
                facultyRepository.findById(id).orElseThrow(() -> new NotFoundException("Faculty not found: " + id))));
    }
}
