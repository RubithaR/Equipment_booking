package com.smartlab.userservice.controller;

import com.smartlab.userservice.dto.DepartmentApprovalChain;
import com.smartlab.userservice.dto.DepartmentResponse;
import com.smartlab.userservice.dto.UserResponse;
import com.smartlab.userservice.entity.Department;
import com.smartlab.security.Roles;
import com.smartlab.userservice.entity.User;
import com.smartlab.security.exception.BadRequestException;
import com.smartlab.security.exception.NotFoundException;
import com.smartlab.userservice.repository.DepartmentRepository;
import com.smartlab.userservice.repository.UserRepository;
import com.smartlab.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<DepartmentResponse>> list(@RequestParam(required = false) Long facultyId) {
        List<Department> rows = (facultyId != null)
                ? departmentRepository.findByFacultyId(facultyId)
                : departmentRepository.findAll();
        return ResponseEntity.ok(rows.stream().map(DepartmentResponse::from).collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DepartmentResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(DepartmentResponse.from(getOrThrow(id)));
    }

    /**
     * The department's approvers used by the booking flow. Today the booking flow
     * routes a request to the department HoD (stage 1) before the lab instructor
     * (stage 2). {@code hod} may be null when the department has no active HoD —
     * the booking service then skips the HoD stage. Resolves the pinned
     * {@code hodUserId} first, then falls back to any active HoD in the department.
     */
    @GetMapping("/{id}/approval-chain")
    public ResponseEntity<DepartmentApprovalChain> approvalChain(@PathVariable Long id) {
        Department dept = getOrThrow(id);
        return ResponseEntity.ok(new DepartmentApprovalChain(resolveHod(dept), null));
    }

    private UserResponse resolveHod(Department dept) {
        if (dept.getHodUserId() != null) {
            User pinned = userRepository.findById(dept.getHodUserId()).orElse(null);
            if (pinned != null
                    && Roles.HOD.equals(pinned.getRole())
                    && UserService.STATUS_ACTIVE.equals(pinned.getStatus())) {
                return UserResponse.from(pinned);
            }
        }
        return userRepository
                .findByRoleAndStatusAndDepartmentId(Roles.HOD, UserService.STATUS_ACTIVE, dept.getId())
                .stream().findFirst().map(UserResponse::from).orElse(null);
    }

    /**
     * Assign (or clear) the HoD on a department. Body: {"hodUserId": <id|null>}.
     * Any active staff member of the department can be picked — if they are not
     * already an HoD they are promoted to the HOD role here, so the booking flow
     * (which routes stage-1 review to the department HoD) recognises them.
     */
    @PatchMapping("/{id}/hod")
    public ResponseEntity<DepartmentResponse> setHod(@PathVariable Long id,
                                                    @RequestBody Map<String, Long> body) {
        Department dept = getOrThrow(id);
        Long newHodId = body == null ? null : body.get("hodUserId");
        if (newHodId != null) {
            User u = userRepository.findById(newHodId)
                    .orElseThrow(() -> new BadRequestException("User not found: " + newHodId));
            if (!UserService.STATUS_ACTIVE.equals(u.getStatus())) {
                throw new BadRequestException("User is not active");
            }
            boolean canHead = Roles.ASSIGNABLE_STAFF.contains(u.getRole())
                    || Roles.STAFF.equals(u.getRole());
            if (!canHead) {
                throw new BadRequestException("Only staff members can be made Head of Department");
            }
            // Any staff member can head this department. Make them the HoD here: ensure the
            // HOD role and move them into this department if they aren't already in it.
            if (!Roles.HOD.equals(u.getRole()) || !dept.getId().equals(u.getDepartmentId())) {
                u.setRole(Roles.HOD);
                u.setDepartmentId(dept.getId());
                userRepository.save(u);
            }
        }
        dept.setHodUserId(newHodId);
        return ResponseEntity.ok(DepartmentResponse.from(departmentRepository.save(dept)));
    }

    private Department getOrThrow(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Department not found: " + id));
    }
}
