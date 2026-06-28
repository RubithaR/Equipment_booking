package com.smartlab.userservice.config;

import com.smartlab.userservice.entity.Department;
import com.smartlab.userservice.entity.Faculty;
import com.smartlab.security.Roles;
import com.smartlab.userservice.entity.User;
import com.smartlab.userservice.repository.DepartmentRepository;
import com.smartlab.userservice.repository.FacultyRepository;
import com.smartlab.userservice.repository.UserRepository;
import com.smartlab.userservice.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * One-shot bootstrap: ensure the Main Admin and four Department Admins exist
 * after Flyway has provisioned the schema and seeded faculties/departments.
 * Idempotent — safe to run on every boot.
 */
@Configuration
public class SystemBootstrap {

    @Value("${admin.seed.email}")    private String mainAdminEmail;
    @Value("${admin.seed.password}") private String mainAdminPassword;
    @Value("${admin.seed.fullName}") private String mainAdminFullName;

    @Value("${seed.dept-admin.computer.email}")    private String ceEmail;
    @Value("${seed.dept-admin.computer.password}") private String cePassword;
    @Value("${seed.dept-admin.electrical.email}")    private String eeEmail;
    @Value("${seed.dept-admin.electrical.password}") private String eePassword;
    @Value("${seed.dept-admin.mechanical.email}")    private String meEmail;
    @Value("${seed.dept-admin.mechanical.password}") private String mePassword;
    @Value("${seed.dept-admin.civil.email}")    private String civEmail;
    @Value("${seed.dept-admin.civil.password}") private String civPassword;

    @Bean
    public CommandLineRunner bootstrap(FacultyRepository facultyRepo,
                                       DepartmentRepository deptRepo,
                                       UserRepository userRepo,
                                       PasswordEncoder encoder) {
        return args -> {
            Faculty foe = facultyRepo.findByCode("FOE")
                    .orElseThrow(() -> new IllegalStateException(
                            "Faculty 'FOE' not seeded — Flyway V2 should have created it."));

            seedAdmin(userRepo, encoder, mainAdminEmail, mainAdminPassword, mainAdminFullName,
                    Roles.MAIN_ADMIN, foe.getId(), null);

            seedDeptAdmin(userRepo, encoder, deptRepo, foe.getId(), "CE",  ceEmail,  cePassword,
                    "Computer Engineering Admin");
            seedDeptAdmin(userRepo, encoder, deptRepo, foe.getId(), "EE",  eeEmail,  eePassword,
                    "Electrical Engineering Admin");
            seedDeptAdmin(userRepo, encoder, deptRepo, foe.getId(), "ME",  meEmail,  mePassword,
                    "Mechanical Engineering Admin");
            seedDeptAdmin(userRepo, encoder, deptRepo, foe.getId(), "CIV", civEmail, civPassword,
                    "Civil Engineering Admin");
        };
    }

    private void seedAdmin(UserRepository repo, PasswordEncoder encoder,
                           String email, String password, String fullName,
                           String role, Long facultyId, Long departmentId) {
        if (isBlank(email) || isBlank(password)) {
            return;
        }
        if (repo.existsByEmail(email.toLowerCase())) {
            System.out.println("[Bootstrap] " + role + " already exists: " + email);
            return;
        }
        User u = new User();
        u.setEmail(email.toLowerCase());
        u.setPassword(encoder.encode(password));
        u.setFullName(fullName);
        u.setRole(role);
        u.setStatus(UserService.STATUS_ACTIVE);
        u.setFacultyId(facultyId);
        u.setDepartmentId(departmentId);
        repo.save(u);
        System.out.println("[Bootstrap] " + role + " created: " + email);
    }

    private void seedDeptAdmin(UserRepository userRepo, PasswordEncoder encoder,
                               DepartmentRepository deptRepo,
                               Long facultyId, String deptCode,
                               String email, String password, String fullName) {
        if (isBlank(email) || isBlank(password)) {
            // Skip silently — admins can be added later via the UI / manual SQL.
            return;
        }
        Department dept = deptRepo.findByFacultyIdAndCode(facultyId, deptCode)
                .orElseThrow(() -> new IllegalStateException(
                        "Department '" + deptCode + "' not seeded — Flyway V2 should have created it."));
        seedAdmin(userRepo, encoder, email, password, fullName,
                Roles.DEPT_ADMIN, facultyId, dept.getId());
    }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
}
