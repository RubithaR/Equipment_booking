package com.smartlab.userservice.repository;

import com.smartlab.userservice.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
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
    List<User> findByRoleInAndStatus(Collection<String> roles, String status);
    List<User> findByRoleAndDepartmentId(String role, Long departmentId);
    List<User> findByRoleAndStatusAndDepartmentId(String role, String status, Long departmentId);
    List<User> findByRoleInAndStatusAndDepartmentId(Collection<String> roles, String status, Long departmentId);
    List<User> findByDepartmentId(Long departmentId);

    @Query("""
            SELECT u FROM User u
            WHERE u.role IN :roles
              AND u.status = 'ACTIVE'
              AND (
                    :q IS NULL OR :q = ''
                    OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(u.email)    LIKE LOWER(CONCAT('%', :q, '%'))
                  )
            ORDER BY u.fullName ASC
            """)
    List<User> searchByRoles(@Param("roles") Collection<String> roles,
                             @Param("q") String q,
                             Pageable pageable);
}
