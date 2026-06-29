package com.smartlab.userservice.service;

import com.smartlab.userservice.dto.AuthResponse;
import com.smartlab.userservice.dto.UserRequest;
import com.smartlab.userservice.dto.UserResponse;
import com.smartlab.userservice.entity.Department;
import com.smartlab.security.Roles;
import com.smartlab.userservice.entity.User;
import com.smartlab.security.exception.AuthenticationException;
import com.smartlab.security.exception.AuthorizationException;
import com.smartlab.security.exception.BadRequestException;
import com.smartlab.security.exception.ConflictException;
import com.smartlab.security.exception.NotFoundException;
import com.smartlab.notificationclient.Notifier;
import com.smartlab.userservice.notifier.NotificationEvent;
import com.smartlab.userservice.repository.DepartmentRepository;
import com.smartlab.userservice.repository.FacultyRepository;
import com.smartlab.userservice.repository.UserRepository;
import com.smartlab.security.CurrentUser;
import com.smartlab.security.UserContext;
import com.smartlab.userservice.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    public static final String STATUS_ACTIVE  = "ACTIVE";
    public static final String STATUS_PENDING = "PENDING";

    // Students must use a Faculty of Engineering email: en + 6 digits @foe.sjp.ac.lk
    private static final Pattern STUDENT_EMAIL = Pattern.compile("^en\\d{6}@foe\\.sjp\\.ac\\.lk$");
    private static final Pattern EN_NUMBER     = Pattern.compile("^EN\\d{6}$");

    private final UserRepository userRepository;
    private final FacultyRepository facultyRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final Notifier<NotificationEvent> notifier;

    public UserResponse register(UserRequest request) {
        String role = request.getRole() == null ? "" : request.getRole().toUpperCase();
        if (!Roles.SELF_REGISTERABLE.contains(role)) {
            throw new BadRequestException(
                    "Role must be STUDENT, or a staff role (INSTRUCTOR, LECTURER, HOD). Admin roles are created by admins.");
        }
        String email = lower(request.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email already registered");
        }
        if (request.getDepartmentId() == null) {
            throw new BadRequestException("Department is required");
        }
        Department dept = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new BadRequestException("Unknown department: " + request.getDepartmentId()));

        // If facultyId is provided, it must match the department's faculty.
        Long facultyId = request.getFacultyId() != null ? request.getFacultyId() : dept.getFacultyId();
        if (!facultyId.equals(dept.getFacultyId())) {
            throw new BadRequestException("Department does not belong to the given faculty");
        }
        if (!facultyRepository.existsById(facultyId)) {
            throw new BadRequestException("Unknown faculty: " + facultyId);
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setRole(role);
        user.setFacultyId(facultyId);
        user.setDepartmentId(dept.getId());
        user.setPhoneNumber(request.getPhoneNumber());

        if (Roles.STUDENT.equals(role)) {
            // Students: enforce EN email format and academic identifiers
            if (!STUDENT_EMAIL.matcher(email).matches()) {
                throw new BadRequestException(
                        "Student email must be in the format en<6-digits>@foe.sjp.ac.lk (e.g. en102020@foe.sjp.ac.lk).");
            }
            if (isBlank(request.getEnNumber()) || isBlank(request.getIndexNumber())
                    || isBlank(request.getNameWithInitial())) {
                throw new BadRequestException(
                        "Student registration requires: enNumber, indexNumber, nameWithInitial");
            }
            String enNumber = request.getEnNumber().toUpperCase();
            if (!EN_NUMBER.matcher(enNumber).matches()) {
                throw new BadRequestException("EN number must be EN followed by 6 digits (e.g. EN102020).");
            }
            if (userRepository.existsByEnNumber(enNumber)) {
                throw new ConflictException("EN number already registered");
            }
            if (userRepository.existsByIndexNumber(request.getIndexNumber())) {
                throw new ConflictException("Index number already registered");
            }
            // uniEmail: keep alongside the login email; default to the same value if not provided.
            String uniEmail = isBlank(request.getUniEmail()) ? email : lower(request.getUniEmail());
            user.setEnNumber(enNumber);
            user.setIndexNumber(request.getIndexNumber());
            user.setNameWithInitial(request.getNameWithInitial());
            user.setUniEmail(uniEmail);
            user.setStatus(STATUS_ACTIVE);
        } else {
            // Staff self-registration (instructor / lecturer / HOD): wait for department admin approval
            user.setStatus(STATUS_PENDING);
        }

        User saved = userRepository.save(user);
        return UserResponse.from(saved);
    }

    public AuthResponse login(String email, String password) {
        User user = userRepository.findByEmail(lower(email))
                .orElseThrow(() -> new NotFoundException("User not found: " + email));
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new AuthenticationException("Invalid password");
        }
        if (STATUS_PENDING.equals(user.getStatus())) {
            throw new AuthenticationException(
                    "Your account is awaiting admin approval. You will be notified once approved.");
        }
        String token = jwtUtil.generateToken(
                user.getEmail(), user.getRole(), user.getId(), user.getFacultyId(), user.getDepartmentId());
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getFullName(),
                user.getRole(), user.getFacultyId(), user.getDepartmentId());
    }

    public UserResponse getById(Long id) {
        return UserResponse.from(userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id)));
    }

    public List<UserResponse> getAll() {
        Long deptScope = deptScopeForCaller();
        List<User> rows = (deptScope == null)
                ? userRepository.findAll()
                : userRepository.findByDepartmentId(deptScope);
        return rows.stream().map(UserResponse::from).collect(Collectors.toList());
    }

    public List<UserResponse> getByRole(String role) {
        Long deptScope = deptScopeForCaller();
        String r = role.toUpperCase();
        List<User> rows = (deptScope == null)
                ? userRepository.findByRole(r)
                : userRepository.findByRoleAndDepartmentId(r, deptScope);
        return rows.stream().map(UserResponse::from).collect(Collectors.toList());
    }

    /** When the caller is a DEPT_ADMIN, return their departmentId so admin lists scope to it. */
    private Long deptScopeForCaller() {
        UserContext me = CurrentUser.get();
        return (me != null && me.hasRole(Roles.DEPT_ADMIN)) ? me.departmentId() : null;
    }

    public List<UserResponse> search(String q, String rolesCsv, int limit) {
        if (rolesCsv == null || rolesCsv.isBlank()) {
            throw new BadRequestException("roles is required (comma-separated)");
        }
        java.util.Set<String> roles = java.util.Arrays.stream(rolesCsv.split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .map(String::toUpperCase).collect(Collectors.toSet());
        for (String r : roles) {
            if (!Roles.isValid(r)) throw new BadRequestException("Unknown role: " + r);
        }
        int capped = Math.max(1, Math.min(limit, 100));
        org.springframework.data.domain.Pageable page =
                org.springframework.data.domain.PageRequest.of(0, capped);
        return userRepository.searchByRoles(roles, q == null ? "" : q.trim(), page).stream()
                .map(UserResponse::from).collect(Collectors.toList());
    }

    /** Pending staff (instructor / lecturer / HOD) awaiting department admin approval. */
    public List<UserResponse> getPendingInstructors(Long departmentId) {
        // DEPT_ADMIN always sees only their own department, even if they pass ?departmentId=
        Long deptScope = deptScopeForCaller();
        Long effective = (deptScope != null) ? deptScope : departmentId;
        List<User> rows = (effective != null)
                ? userRepository.findByRoleInAndStatusAndDepartmentId(Roles.STAFF, STATUS_PENDING, effective)
                : userRepository.findByRoleInAndStatus(Roles.STAFF, STATUS_PENDING);
        return rows.stream().map(UserResponse::from).collect(Collectors.toList());
    }

    /** Approve any pending staff member (instructor / lecturer / HOD). */
    public UserResponse approveInstructor(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));
        if (!Roles.STAFF.contains(user.getRole())) {
            throw new BadRequestException("User is not a staff member");
        }
        ensureCallerCanManage(user);
        if (STATUS_ACTIVE.equals(user.getStatus())) {
            throw new ConflictException("This account is already active");
        }
        user.setStatus(STATUS_ACTIVE);
        User saved = userRepository.save(user);

        notifier.publish(new NotificationEvent.InstructorApproved(saved.getId()));
        return UserResponse.from(saved);
    }

    /** Reject (delete) a pending staff application. */
    public void rejectInstructor(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));
        if (!Roles.STAFF.contains(user.getRole())) {
            throw new BadRequestException("User is not a staff member");
        }
        ensureCallerCanManage(user);
        userRepository.delete(user);
    }

    /** Department admins can only act on users that belong to their own department. */
    private void ensureCallerCanManage(User target) {
        Long deptScope = deptScopeForCaller();
        if (deptScope == null) return; // MAIN_ADMIN
        if (target.getDepartmentId() == null || !deptScope.equals(target.getDepartmentId())) {
            throw new AuthorizationException("Department admins can only manage users in their own department");
        }
    }

    public List<UserResponse> getPendingStudentsForInstructor(Long instructorId) {
        User instructor = userRepository.findById(instructorId)
                .orElseThrow(() -> new NotFoundException("Instructor not found with id: " + instructorId));
        if (!Roles.LECTURER.equals(instructor.getRole())) {
            throw new BadRequestException("User is not a lecturer");
        }
        Long deptId = instructor.getDepartmentId();
        List<User> rows = userRepository.findByRoleAndStatusAndDepartmentId(Roles.STUDENT, STATUS_PENDING, deptId);
        return rows.stream().map(UserResponse::from).collect(Collectors.toList());
    }

    public UserResponse approveStudent(Long id, Long instructorId) {
        User student = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));
        if (!Roles.STUDENT.equals(student.getRole())) {
            throw new BadRequestException("User is not a student");
        }
        User instructor = userRepository.findById(instructorId)
                .orElseThrow(() -> new NotFoundException("Instructor not found with id: " + instructorId));
        if (!Roles.LECTURER.equals(instructor.getRole())) {
            throw new BadRequestException("User is not a lecturer");
        }
        if (!student.getDepartmentId().equals(instructor.getDepartmentId())) {
            throw new AuthorizationException("Lecturer can only approve students in their own department");
        }
        if (STATUS_ACTIVE.equals(student.getStatus())) {
            throw new ConflictException("Student is already active");
        }
        student.setStatus(STATUS_ACTIVE);
        User saved = userRepository.save(student);
        return UserResponse.from(saved);
    }

    public void rejectStudent(Long id, Long instructorId) {
        User student = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));
        if (!Roles.STUDENT.equals(student.getRole())) {
            throw new BadRequestException("User is not a student");
        }
        User instructor = userRepository.findById(instructorId)
                .orElseThrow(() -> new NotFoundException("Instructor not found with id: " + instructorId));
        if (!Roles.LECTURER.equals(instructor.getRole())) {
            throw new BadRequestException("User is not a lecturer");
        }
        if (!student.getDepartmentId().equals(instructor.getDepartmentId())) {
            throw new AuthorizationException("Lecturer can only reject students in their own department");
        }
        userRepository.delete(student);
    }

    public Map<String, Boolean> checkAvailability(String email, String enNumber, String indexNumber) {
        Map<String, Boolean> result = new HashMap<>();
        result.put("emailTaken",  !isBlank(email)      && userRepository.existsByEmail(lower(email)));
        result.put("enTaken",     !isBlank(enNumber)   && userRepository.existsByEnNumber(enNumber.toUpperCase()));
        result.put("indexTaken",  !isBlank(indexNumber)&& userRepository.existsByIndexNumber(indexNumber));
        return result;
    }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
    private static String lower(String s)    { return s == null ? null : s.trim().toLowerCase(); }
}
