package com.smartlab.equipmentservice.repository;

import com.smartlab.equipmentservice.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {
    List<Item> findByLabId(Long labId);
    List<Item> findByLabIdIn(List<Long> labIds);
    List<Item> findByStatus(String status);
    List<Item> findByCategory(String category);
    List<Item> findByModel(String model);
}
