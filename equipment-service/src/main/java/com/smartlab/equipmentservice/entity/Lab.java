package com.smartlab.equipmentservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "labs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Lab {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "department_id", nullable = false)
    private Long departmentId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 200)
    private String location;

    @Column(columnDefinition = "TEXT")
    private String description;

    // One instructor per lab (nullable until assigned). One instructor can hold many labs.
    @Column(name = "instructor_user_id")
    private Long instructorUserId;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private Instant createdAt;
}
