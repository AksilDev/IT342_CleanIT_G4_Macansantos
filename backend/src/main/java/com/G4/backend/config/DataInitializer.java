package com.G4.backend.config;

import com.G4.backend.entity.User;
import com.G4.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Configuration
public class DataInitializer {

    private final AdminConfig adminConfig;

    public DataInitializer(AdminConfig adminConfig) {
        this.adminConfig = adminConfig;
    }

    @Bean
    public CommandLineRunner initData(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // Check if super admin already exists
            if (!userRepository.existsByEmail(adminConfig.getAdminEmail())) {
                User superAdmin = new User();
                superAdmin.setName(adminConfig.getAdminName());
                superAdmin.setEmail(adminConfig.getAdminEmail());
                superAdmin.setPasswordHash(passwordEncoder.encode(adminConfig.getAdminPassword()));
                superAdmin.setRole("admin");
                superAdmin.setContactNo("0000000000");
                superAdmin.setVerified(true);
                superAdmin.setCreatedAt(LocalDateTime.now());
                
                userRepository.save(superAdmin);
                System.out.println("Super admin account created successfully!");
                System.out.println("Email: " + adminConfig.getAdminEmail());
                System.out.println("Password: " + adminConfig.getAdminPassword());
            } else {
                System.out.println("Super admin account already exists with email: " + adminConfig.getAdminEmail());
            }
        };
    }
}
