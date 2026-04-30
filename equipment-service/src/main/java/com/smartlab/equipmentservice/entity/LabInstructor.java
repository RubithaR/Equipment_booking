package com.smartlab.equipmentservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "lab_instructors", uniqueConstraints = {
        @UniqueConstraint(name = "uk_lab_instructor", columnNames = {"labId", "instructorId"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LabInstructor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long labId;

    @Column(nullable = false)
    private Long instructorId;
}
