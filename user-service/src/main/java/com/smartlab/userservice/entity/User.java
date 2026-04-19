package com.smartlab.userservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Login email — gmail or any email allowed
    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String fullName;

    // STUDENT, INSTRUCTOR, ADMIN
    @Column(nullable = false)
    private String role;

    private String department;

    // ACTIVE, PENDING (instructor needs admin approval before login)
    @Column(nullable = false)
    private String status;

    private String phoneNumber;

    // ===== Student-only fields (null for instructor/admin) =====
    private String enNumber;        // e.g., EN102752
    private String indexNumber;     // e.g., 21ENG009
    private String nameWithInitial; // e.g., A. Harishan
    private String uniEmail;        // e.g., en102752@feo.sjp.ac.lk

    // TODO: ID photo — store URL/path here once file upload is implemented
    private String idPhotoUrl;
}
