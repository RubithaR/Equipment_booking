package com.smartlab.equipmentservice.service;

import com.smartlab.equipmentservice.dto.EquipmentRequest;
import com.smartlab.equipmentservice.entity.Equipment;
import com.smartlab.equipmentservice.repository.EquipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;

    public Equipment create(EquipmentRequest request) {
        Equipment equipment = new Equipment();
        equipment.setName(request.getName());
        equipment.setCategory(request.getCategory());
        equipment.setLocation(request.getLocation());
        equipment.setStatus(request.getStatus());
        equipment.setDescription(request.getDescription());
        return equipmentRepository.save(equipment);
    }

    public List<Equipment> getAll() {
        return equipmentRepository.findAll();
    }

    public Equipment getById(Long id) {
        return equipmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Equipment not found with id: " + id));
    }

    public Equipment updateStatus(Long id, String status) {
        Equipment equipment = getById(id);
        equipment.setStatus(status);
        return equipmentRepository.save(equipment);
    }

    public Equipment update(Long id, EquipmentRequest request) {
        Equipment equipment = getById(id);
        equipment.setName(request.getName());
        equipment.setCategory(request.getCategory());
        equipment.setLocation(request.getLocation());
        equipment.setStatus(request.getStatus());
        equipment.setDescription(request.getDescription());
        return equipmentRepository.save(equipment);
    }

    public void delete(Long id) {
        if (!equipmentRepository.existsById(id)) {
            throw new RuntimeException("Equipment not found with id: " + id);
        }
        equipmentRepository.deleteById(id);
    }

    public List<Equipment> getByStatus(String status) {
        return equipmentRepository.findByStatus(status);
    }
}
