package com.smartlab.userservice.config;

import com.smartlab.userservice.entity.User;
import com.smartlab.userservice.repository.UserRepository;
import com.smartlab.userservice.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminSeeder {

    @Value("${admin.seed.email}")
    private String adminEmail;

    @Value("${admin.seed.password}")
    private String adminPassword;

    @Value("${admin.seed.fullName}")
    private String adminFullName;

    @Bean
    public CommandLineRunner seedAdmin(UserRepository repo, PasswordEncoder encoder) {
        return args -> {
            if (repo.existsByEmail(adminEmail)) {
                System.out.println("[AdminSeeder] Admin already exists: " + adminEmail);
                return;
            }
            User admin = new User();
            admin.setEmail(adminEmail);
            admin.setPassword(encoder.encode(adminPassword));
            admin.setFullName(adminFullName);
            admin.setRole(UserService.ROLE_ADMIN);
            admin.setStatus(UserService.STATUS_ACTIVE);
            admin.setDepartment("Administration");
            repo.save(admin);
            System.out.println("[AdminSeeder] Admin created: " + adminEmail + " / " + adminPassword);
        };
    }
}
