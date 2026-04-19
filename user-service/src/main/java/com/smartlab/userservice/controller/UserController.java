package com.smartlab.userservice.controller;

import com.smartlab.userservice.dto.AuthResponse;
import com.smartlab.userservice.dto.UserRequest;
import com.smartlab.userservice.dto.UserResponse;
import com.smartlab.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody UserRequest request) {
        return ResponseEntity.ok(userService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(userService.login(body.get("email"), body.get("password")));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAll() {
        return ResponseEntity.ok(userService.getAll());
    }

    // Filter users by role: /api/users/by-role/STUDENT or /INSTRUCTOR
    @GetMapping("/by-role/{role}")
    public ResponseEntity<List<UserResponse>> getByRole(@PathVariable String role) {
        return ResponseEntity.ok(userService.getByRole(role));
    }

    // Admin-only: list instructors waiting for approval
    @GetMapping("/instructors/pending")
    public ResponseEntity<List<UserResponse>> getPendingInstructors() {
        return ResponseEntity.ok(userService.getPendingInstructors());
    }

    // Admin-only: approve a pending instructor
    @PatchMapping("/{id}/approve")
    public ResponseEntity<UserResponse> approve(@PathVariable Long id) {
        return ResponseEntity.ok(userService.approveInstructor(id));
    }

    // Admin-only: reject (delete) an instructor application
    @DeleteMapping("/{id}/reject")
    public ResponseEntity<Void> reject(@PathVariable Long id) {
        userService.rejectInstructor(id);
        return ResponseEntity.noContent().build();
    }
}
