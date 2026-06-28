package com.smartlab.equipmentservice.repository;

import com.smartlab.equipmentservice.entity.Lab;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LabRepository extends JpaRepository<Lab, Long> {
    List<Lab> findByDepartmentId(Long departmentId);
    List<Lab> findByInstructorUserId(Long instructorUserId);
    Optional<Lab> findByDepartmentIdAndName(Long departmentId, String name);
    boolean existsByDepartmentIdAndName(Long departmentId, String name);
}
