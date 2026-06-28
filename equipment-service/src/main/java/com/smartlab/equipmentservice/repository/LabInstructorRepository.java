package com.smartlab.equipmentservice.repository;

import com.smartlab.equipmentservice.entity.LabInstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LabInstructorRepository extends JpaRepository<LabInstructor, Long> {
    List<LabInstructor> findByLabId(Long labId);
    List<LabInstructor> findByInstructorId(Long instructorId);
    Optional<LabInstructor> findByLabIdAndInstructorId(Long labId, Long instructorId);
    boolean existsByLabIdAndInstructorId(Long labId, Long instructorId);
    void deleteByLabId(Long labId);
}
