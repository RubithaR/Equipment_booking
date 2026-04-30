package com.smartlab.userservice.repository;

import com.smartlab.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByEnNumber(String enNumber);
    boolean existsByIndexNumber(String indexNumber);
    List<User> findByRole(String role);
    List<User> findByRoleAndStatus(String role, String status);
    List<User> findByRoleAndStatusAndDepartment(String role, String status, String department);
}
