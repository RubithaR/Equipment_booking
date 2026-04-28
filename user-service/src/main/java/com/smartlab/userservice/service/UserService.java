package com.smartlab.userservice.service;

import com.smartlab.userservice.client.NotificationClient;
import com.smartlab.userservice.dto.AuthResponse;
import com.smartlab.userservice.dto.UserRequest;
import com.smartlab.userservice.dto.UserResponse;
import com.smartlab.userservice.entity.User;
import com.smartlab.userservice.exception.BadRequestException;
import com.smartlab.userservice.exception.ConflictException;
import com.smartlab.userservice.exception.NotFoundException;
import com.smartlab.userservice.exception.UnauthorizedException;
import com.smartlab.userservice.repository.UserRepository;
import com.smartlab.userservice.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    public static final String ROLE_STUDENT = "STUDENT";
    public static final String ROLE_INSTRUCTOR = "INSTRUCTOR";
    public static final String ROLE_ADMIN = "ADMIN";

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_PENDING = "PENDING";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final NotificationClient notificationClient;

    public UserResponse register(UserRequest request) {
        String role = request.getRole() == null ? "" : request.getRole().toUpperCase();
        if (!role.equals(ROLE_STUDENT) && !role.equals(ROLE_INSTRUCTOR)) {
            throw new BadRequestException("Role must be STUDENT or INSTRUCTOR (admin is preset).");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already registered");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setRole(role);
        user.setDepartment(request.getDepartment());
        user.setPhoneNumber(request.getPhoneNumber());

        if (role.equals(ROLE_STUDENT)) {
            // Students: collect extra academic info, instantly active
            if (isBlank(request.getEnNumber()) || isBlank(request.getIndexNumber())
                    || isBlank(request.getNameWithInitial()) || isBlank(request.getUniEmail())) {
                throw new BadRequestException(
                        "Student registration requires: enNumber, indexNumber, nameWithInitial, uniEmail");
            }
            if (userRepository.existsByEnNumber(request.getEnNumber())) {
                throw new ConflictException("EN number already registered");
            }
            if (userRepository.existsByIndexNumber(request.getIndexNumber())) {
                throw new ConflictException("Index number already registered");
            }
            user.setEnNumber(request.getEnNumber());
            user.setIndexNumber(request.getIndexNumber());
            user.setNameWithInitial(request.getNameWithInitial());
            user.setUniEmail(request.getUniEmail());
            user.setStatus(STATUS_ACTIVE);
        } else {
            // Instructor: must wait for admin approval
            user.setStatus(STATUS_PENDING);
        }

        User saved = userRepository.save(user);
        return UserResponse.from(saved);
    }

    public AuthResponse login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found: " + email));
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new UnauthorizedException("Invalid password");
        }
        if (STATUS_PENDING.equals(user.getStatus())) {
            throw new UnauthorizedException(
                    "Your instructor account is awaiting admin approval. You will be notified once approved.");
        }
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole(), user.getId());
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getFullName(), user.getRole());
    }

    public UserResponse getById(Long id) {
        return UserResponse.from(userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id)));
    }

    public List<UserResponse> getAll() {
        return userRepository.findAll().stream().map(UserResponse::from).collect(Collectors.toList());
    }

    public List<UserResponse> getByRole(String role) {
        return userRepository.findByRole(role.toUpperCase()).stream()
                .map(UserResponse::from).collect(Collectors.toList());
    }

    public List<UserResponse> getPendingInstructors() {
        return userRepository.findByRoleAndStatus(ROLE_INSTRUCTOR, STATUS_PENDING).stream()
                .map(UserResponse::from).collect(Collectors.toList());
    }

    public UserResponse approveInstructor(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));
        if (!ROLE_INSTRUCTOR.equals(user.getRole())) {
            throw new BadRequestException("User is not an instructor");
        }
        if (STATUS_ACTIVE.equals(user.getStatus())) {
            throw new ConflictException("Instructor is already active");
        }
        user.setStatus(STATUS_ACTIVE);
        User saved = userRepository.save(user);

        // Notify the instructor (in-app notification — also serves as the "permission email")
        try {
            notificationClient.send(Map.of(
                    "userId", saved.getId(),
                    "title", "Instructor account approved",
                    "message", "Your account has been approved by admin. You can now log in.",
                    "type", "ACCOUNT_APPROVED"
            ));
        } catch (Exception e) {
            System.err.println("Warning: failed to send approval notification: " + e.getMessage());
        }
        return UserResponse.from(saved);
    }

    public void rejectInstructor(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));
        if (!ROLE_INSTRUCTOR.equals(user.getRole())) {
            throw new BadRequestException("User is not an instructor");
        }
        userRepository.delete(user);
    }

    public Map<String, Boolean> checkAvailability(String email, String enNumber, String indexNumber) {
        Map<String, Boolean> result = new HashMap<>();
        result.put("emailTaken", !isBlank(email) && userRepository.existsByEmail(email));
        result.put("enTaken", !isBlank(enNumber) && userRepository.existsByEnNumber(enNumber));
        result.put("indexTaken", !isBlank(indexNumber) && userRepository.existsByIndexNumber(indexNumber));
        return result;
    }

    private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
}
