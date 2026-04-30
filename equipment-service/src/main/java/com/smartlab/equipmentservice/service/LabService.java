package com.smartlab.equipmentservice.service;

import com.smartlab.equipmentservice.client.UserClient;
import com.smartlab.equipmentservice.dto.LabRequest;
import com.smartlab.equipmentservice.dto.LabResponse;
import com.smartlab.equipmentservice.dto.UserDto;
import com.smartlab.equipmentservice.entity.Lab;
import com.smartlab.equipmentservice.entity.LabInstructor;
import com.smartlab.equipmentservice.repository.LabInstructorRepository;
import com.smartlab.equipmentservice.repository.LabRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LabService {

    private static final Logger log = LoggerFactory.getLogger(LabService.class);

    private final LabRepository labRepository;
    private final LabInstructorRepository labInstructorRepository;
    private final UserClient userClient;

    public LabResponse create(LabRequest request) {
        if (labRepository.existsByName(request.getName())) {
            throw new IllegalStateException("Lab name already exists: " + request.getName());
        }
        Lab lab = new Lab();
        lab.setName(request.getName());
        Lab saved = labRepository.save(lab);
        return LabResponse.fromEntity(saved, List.of());
    }

    public List<LabResponse> getAll() {
        return labRepository.findAll().stream()
                .map(lab -> LabResponse.fromEntity(lab, fetchInstructors(lab.getId())))
                .toList();
    }

    public LabResponse getById(Long id) {
        Lab lab = labRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lab not found with id: " + id));
        return LabResponse.fromEntity(lab, fetchInstructors(lab.getId()));
    }

    @Transactional
    public void delete(Long id) {
        if (!labRepository.existsById(id)) {
            throw new IllegalArgumentException("Lab not found with id: " + id);
        }
        labInstructorRepository.deleteByLabId(id);
        labRepository.deleteById(id);
    }

    public LabResponse assignInstructor(Long labId, Long instructorId) {
        Lab lab = labRepository.findById(labId)
                .orElseThrow(() -> new IllegalArgumentException("Lab not found with id: " + labId));

        // Verify instructor exists and has the right role
        UserDto user;
        try {
            user = userClient.getUserById(instructorId);
        } catch (Exception e) {
            throw new IllegalArgumentException("Instructor not found with id: " + instructorId);
        }
        if (!"INSTRUCTOR".equalsIgnoreCase(user.getRole())) {
            throw new IllegalArgumentException("User is not an instructor");
        }
        if (labInstructorRepository.existsByLabIdAndInstructorId(labId, instructorId)) {
            throw new IllegalStateException("Instructor already assigned to this lab");
        }

        LabInstructor link = new LabInstructor();
        link.setLabId(labId);
        link.setInstructorId(instructorId);
        labInstructorRepository.save(link);

        return LabResponse.fromEntity(lab, fetchInstructors(labId));
    }

    public void unassignInstructor(Long labId, Long instructorId) {
        LabInstructor link = labInstructorRepository.findByLabIdAndInstructorId(labId, instructorId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Instructor " + instructorId + " is not assigned to lab " + labId));
        labInstructorRepository.delete(link);
    }

    public List<LabResponse> getLabsForInstructor(Long instructorId) {
        List<LabInstructor> links = labInstructorRepository.findByInstructorId(instructorId);
        List<Long> labIds = links.stream().map(LabInstructor::getLabId).toList();
        return labRepository.findAllById(labIds).stream()
                .map(lab -> LabResponse.fromEntity(lab, fetchInstructors(lab.getId())))
                .toList();
    }

    public boolean isInstructorAssignedToLab(Long instructorId, Long labId) {
        return labInstructorRepository.existsByLabIdAndInstructorId(labId, instructorId);
    }

    // ===== helpers =====

    private List<LabResponse.InstructorBrief> fetchInstructors(Long labId) {
        List<LabInstructor> links = labInstructorRepository.findByLabId(labId);
        List<LabResponse.InstructorBrief> result = new ArrayList<>();
        for (LabInstructor link : links) {
            try {
                UserDto user = userClient.getUserById(link.getInstructorId());
                result.add(new LabResponse.InstructorBrief(
                        user.getId(), user.getFullName(), user.getDepartment()));
            } catch (Exception e) {
                log.warn("Failed to fetch instructor id={} for lab id={}", link.getInstructorId(), labId, e);
                result.add(new LabResponse.InstructorBrief(
                        link.getInstructorId(), "(unknown)", null));
            }
        }
        return result;
    }
}
