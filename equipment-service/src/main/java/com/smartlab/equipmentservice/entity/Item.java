package com.smartlab.equipmentservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lab_id", nullable = false)
    private Long labId;

    @Column(nullable = false, length = 200)
    private String model;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(name = "serial_number", unique = true, length = 100)
    private String serialNumber;

    // AVAILABLE, IN_USE, MAINTENANCE, OUT_OF_SERVICE
    @Column(nullable = false, length = 40)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "condition_note", columnDefinition = "TEXT")
    private String conditionNote;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private Instant createdAt;
}
