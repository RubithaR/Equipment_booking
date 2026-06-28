package com.smartlab.userservice.repository;

import com.smartlab.userservice.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    List<Department> findByFacultyId(Long facultyId);
    Optional<Department> findByCode(String code);
    Optional<Department> findByFacultyIdAndCode(Long facultyId, String code);
}
