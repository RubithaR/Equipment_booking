package com.smartlab.userservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 200)
    private String email;

    @Column(nullable = false, length = 200)
    private String password;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    // MAIN_ADMIN, DEPT_ADMIN, HOD, LECTURER, INSTRUCTOR, STUDENT
    @Column(nullable = false, length = 40)
    private String role;

    // ACTIVE, PENDING (instructor accounts wait for dept-admin approval)
    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "faculty_id")
    private Long facultyId;

    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    // ===== Student-only fields (null for staff/admin) =====
    @Column(name = "en_number", length = 20)
    private String enNumber;

    @Column(name = "index_number", length = 20)
    private String indexNumber;

    @Column(name = "name_with_initial", length = 200)
    private String nameWithInitial;

    @Column(name = "uni_email", length = 200)
    private String uniEmail;

    @Column(name = "id_photo_url", length = 500)
    private String idPhotoUrl;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private Instant createdAt;
}
