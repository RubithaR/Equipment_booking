package com.smartlab.equipmentservice.service;

import com.smartlab.equipmentservice.client.UserClient;
import com.smartlab.equipmentservice.dto.EquipmentRequest;
import com.smartlab.equipmentservice.dto.EquipmentResponse;
import com.smartlab.equipmentservice.dto.UserDto;
import com.smartlab.equipmentservice.entity.Equipment;
import com.smartlab.equipmentservice.entity.Lab;
import com.smartlab.equipmentservice.entity.LabInstructor;
import com.smartlab.equipmentservice.repository.EquipmentRepository;
import com.smartlab.equipmentservice.repository.LabInstructorRepository;
import com.smartlab.equipmentservice.repository.LabRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EquipmentService {

    private static final Logger log = LoggerFactory.getLogger(EquipmentService.class);

    private final EquipmentRepository equipmentRepository;
    private final LabRepository labRepository;
    private final LabInstructorRepository labInstructorRepository;
    private final UserClient userClient;

    public EquipmentResponse create(EquipmentRequest request) {
        // Lab is required when creating equipment (otherwise no instructor "owns" it)
        if (request.getLabId() == null) {
            throw new IllegalArgumentException("labId is required");
        }
        if (request.getInstructorId() == null) {
            throw new IllegalArgumentException("instructorId is required");
        }
        if (!labRepository.existsById(request.getLabId())) {
            throw new IllegalArgumentException("Lab not found with id: " + request.getLabId());
        }
        if (!labInstructorRepository.existsByLabIdAndInstructorId(request.getLabId(), request.getInstructorId())) {
            throw new IllegalStateException(
                    "You are not assigned to this lab. Ask the admin to assign you first.");
        }

        Equipment equipment = new Equipment();
        equipment.setName(request.getName());
        equipment.setCategory(request.getCategory());
        equipment.setLocation(request.getLocation());
        equipment.setStatus(request.getStatus());
        equipment.setDescription(request.getDescription());
        equipment.setLabId(request.getLabId());
        Equipment saved = equipmentRepository.save(equipment);
        return enrich(saved);
    }

    public List<EquipmentResponse> getAll() {
        List<Equipment> all = equipmentRepository.findAll();
        return enrichBatch(all);
    }

    public EquipmentResponse getById(Long id) {
        Equipment e = equipmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Equipment not found with id: " + id));
        return enrich(e);
    }

    public EquipmentResponse updateStatus(Long id, String status) {
        Equipment equipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Equipment not found with id: " + id));
        equipment.setStatus(status);
        return enrich(equipmentRepository.save(equipment));
    }

    public EquipmentResponse update(Long id, EquipmentRequest request) {
        Equipment equipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Equipment not found with id: " + id));
        equipment.setName(request.getName());
        equipment.setCategory(request.getCategory());
        equipment.setLocation(request.getLocation());
        equipment.setStatus(request.getStatus());
        equipment.setDescription(request.getDescription());
        if (request.getLabId() != null) {
            equipment.setLabId(request.getLabId());
        }
        return enrich(equipmentRepository.save(equipment));
    }

    public void delete(Long id) {
        if (!equipmentRepository.existsById(id)) {
            throw new RuntimeException("Equipment not found with id: " + id);
        }
        equipmentRepository.deleteById(id);
    }

    public List<EquipmentResponse> getByStatus(String status) {
        return enrichBatch(equipmentRepository.findByStatus(status));
    }

    // Returns the raw Equipment for inter-service Feign calls (booking-service uses this)
    public Equipment getRawById(Long id) {
        return equipmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Equipment not found with id: " + id));
    }

    // ===== response enrichment =====

    private EquipmentResponse enrich(Equipment e) {
        return enrichBatch(List.of(e)).get(0);
    }

    /**
     * Batch-enrich equipment with lab name + instructor names. Caches lab and instructor
     * lookups to avoid duplicate Feign calls when listing many items.
     */
    private List<EquipmentResponse> enrichBatch(List<Equipment> items) {
        // Cache: labId -> Lab name; labId -> instructor names
        Map<Long, String> labNameCache = new HashMap<>();
        Map<Long, List<String>> instructorNamesCache = new HashMap<>();

        List<EquipmentResponse> out = new ArrayList<>();
        for (Equipment e : items) {
            EquipmentResponse r = EquipmentResponse.fromEquipment(e);
            Long labId = e.getLabId();
            if (labId != null) {
                r.setLabName(labNameCache.computeIfAbsent(labId, this::resolveLabName));
                r.setInstructorNames(instructorNamesCache.computeIfAbsent(labId, this::resolveInstructorNames));
            }
            out.add(r);
        }
        return out;
    }

    private String resolveLabName(Long labId) {
        return labRepository.findById(labId).map(Lab::getName).orElse(null);
    }

    private List<String> resolveInstructorNames(Long labId) {
        List<LabInstructor> links = labInstructorRepository.findByLabId(labId);
        List<String> names = new ArrayList<>();
        for (LabInstructor link : links) {
            try {
                UserDto u = userClient.getUserById(link.getInstructorId());
                names.add(u.getFullName());
            } catch (Exception ex) {
                log.warn("Failed to fetch instructor id={} for lab id={}", link.getInstructorId(), labId, ex);
                names.add("(unknown)");
            }
        }
        return names;
    }
}
