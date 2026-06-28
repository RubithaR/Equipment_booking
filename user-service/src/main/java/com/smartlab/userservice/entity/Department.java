package com.smartlab.userservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "departments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "faculty_id", nullable = false)
    private Long facultyId;

    @Column(nullable = false, length = 20)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    // Optional: pinned HoD for this department; used as supervisor-default in Phase 4
    @Column(name = "hod_user_id")
    private Long hodUserId;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private Instant createdAt;
}
