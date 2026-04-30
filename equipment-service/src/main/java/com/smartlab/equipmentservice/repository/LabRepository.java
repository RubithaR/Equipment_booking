package com.smartlab.equipmentservice.repository;

import com.smartlab.equipmentservice.entity.Lab;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LabRepository extends JpaRepository<Lab, Long> {
    boolean existsByName(String name);
}
