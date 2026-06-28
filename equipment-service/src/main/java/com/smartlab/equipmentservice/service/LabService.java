package com.smartlab.equipmentservice.service;

import com.smartlab.equipmentservice.client.UserClient;
import com.smartlab.equipmentservice.client.UserSummary;
import com.smartlab.equipmentservice.dto.LabRequest;
import com.smartlab.equipmentservice.dto.LabResponse;
import com.smartlab.equipmentservice.entity.Lab;
import com.smartlab.security.Roles;
import com.smartlab.security.exception.BadRequestException;
import com.smartlab.security.exception.ConflictException;
import com.smartlab.security.exception.NotFoundException;
import com.smartlab.equipmentservice.repository.LabRepository;
import com.smartlab.security.CurrentUser;
import com.smartlab.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LabService {

    private static final String STATUS_ACTIVE = "ACTIVE";

    private final LabRepository labRepository;
    private final UserClient userClient;

    public LabResponse create(LabRequest request) {
        UserContext me = CurrentUser.require();
        ensureCanManageDepartment(me, request.getDepartmentId());

        if (labRepository.existsByDepartmentIdAndName(request.getDepartmentId(), request.getName())) {
            throw new ConflictException("A lab with that name already exists in this department");
        }
        if (request.getInstructorUserId() != null) {
            verifyInstructor(request.getInstructorUserId(), request.getDepartmentId());
        }

        Lab lab = new Lab();
        lab.setDepartmentId(request.getDepartmentId());
        lab.setName(request.getName());
        lab.setLocation(request.getLocation());
        lab.setDescription(request.getDescription());
        lab.setInstructorUserId(request.getInstructorUserId());
        return LabResponse.from(labRepository.save(lab));
    }

    public List<LabResponse> list(Long departmentId, Long instructorUserId) {
        List<Lab> rows;
        if (departmentId != null) {
            rows = labRepository.findByDepartmentId(departmentId);
        } else if (instructorUserId != null) {
            rows = labRepository.findByInstructorUserId(instructorUserId);
        } else {
            rows = labRepository.findAll();
        }
        return rows.stream().map(LabResponse::from).collect(Collectors.toList());
    }

    public LabResponse getById(Long id) {
        return LabResponse.from(getOrThrow(id));
    }

    public LabResponse update(Long id, LabRequest request) {
        UserContext me = CurrentUser.require();
        Lab lab = getOrThrow(id);
        ensureCanManageDepartment(me, lab.getDepartmentId());

        if (request.getDepartmentId() != null && !request.getDepartmentId().equals(lab.getDepartmentId())) {
            throw new BadRequestException("Lab cannot be moved between departments");
        }
        if (!lab.getName().equals(request.getName())
                && labRepository.existsByDepartmentIdAndName(lab.getDepartmentId(), request.getName())) {
            throw new ConflictException("A lab with that name already exists in this department");
        }
        if (request.getInstructorUserId() != null) {
            verifyInstructor(request.getInstructorUserId(), lab.getDepartmentId());
        }

        lab.setName(request.getName());
        lab.setLocation(request.getLocation());
        lab.setDescription(request.getDescription());
        lab.setInstructorUserId(request.getInstructorUserId());
        return LabResponse.from(labRepository.save(lab));
    }

    public LabResponse assignInstructor(Long labId, Long instructorUserId) {
        UserContext me = CurrentUser.require();
        Lab lab = getOrThrow(labId);
        ensureCanManageDepartment(me, lab.getDepartmentId());

        if (instructorUserId != null) {
            verifyInstructor(instructorUserId, lab.getDepartmentId());
        }
        lab.setInstructorUserId(instructorUserId);
        return LabResponse.from(labRepository.save(lab));
    }

    public void delete(Long id) {
        UserContext me = CurrentUser.require();
        Lab lab = getOrThrow(id);
        ensureCanManageDepartment(me, lab.getDepartmentId());
        labRepository.delete(lab);
    }

    // ===== helpers =====

    private Lab getOrThrow(Long id) {
        return labRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Lab not found: " + id));
    }

    private void ensureCanManageDepartment(UserContext me, Long departmentId) {
        if (me.hasRole(Roles.MAIN_ADMIN)) return;
        if (me.hasRole(Roles.DEPT_ADMIN) && departmentId != null && departmentId.equals(me.departmentId())) return;
        throw new BadRequestException("You can only manage labs within your own department");
    }

    private void verifyInstructor(Long userId, Long departmentId) {
        UserSummary u;
        try {
            u = userClient.getUserById(userId);
        } catch (Exception e) {
            throw new BadRequestException("Instructor not found: " + userId);
        }
        if (u == null) throw new BadRequestException("Instructor not found: " + userId);
        if (!Roles.INSTRUCTOR.equals(u.getRole())) {
            throw new BadRequestException("User " + userId + " is not an instructor");
        }
        if (!STATUS_ACTIVE.equals(u.getStatus())) {
            throw new BadRequestException("Instructor is not active yet");
        }
        if (u.getDepartmentId() != null && departmentId != null
                && !u.getDepartmentId().equals(departmentId)) {
            throw new BadRequestException("Instructor belongs to a different department");
        }
    }
}
