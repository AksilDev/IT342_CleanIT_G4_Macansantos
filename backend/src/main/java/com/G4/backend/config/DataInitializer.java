package com.G4.backend.config;

import com.G4.backend.entity.*;
import com.G4.backend.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Configuration
public class DataInitializer {

    private final AdminConfig adminConfig;

    public DataInitializer(AdminConfig adminConfig) {
        this.adminConfig = adminConfig;
    }

    @Bean
    public CommandLineRunner initData(
            UserRepository userRepository, 
            PasswordEncoder passwordEncoder,
            ServiceRepository serviceRepository,
            AddOnRepository addOnRepository,
            ServiceAllowedAddonRepository serviceAllowedAddonRepository,
            ChecklistItemRepository checklistItemRepository) {
        return args -> {
            // Initialize Admin Account
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
                System.out.println("✓ Super admin account created successfully!");
                System.out.println("  Email: " + adminConfig.getAdminEmail());
                System.out.println("  Password: " + adminConfig.getAdminPassword());
            } else {
                System.out.println("✓ Super admin account already exists");
            }

            // Initialize Services
            System.out.println("\n=== Initializing Services ===");
            Service externalCleaning = serviceRepository.findByName("Standard External Cleaning");
            if (externalCleaning == null) {
                externalCleaning = new Service("Standard External Cleaning", "Complete external cleaning service", 90, 200.0, true);
                serviceRepository.save(externalCleaning);
                System.out.println("✓ Created: Standard External Cleaning (₱200)");
            }

            Service deepCleaning = serviceRepository.findByName("Deep Internal Cleaning");
            if (deepCleaning == null) {
                deepCleaning = new Service("Deep Internal Cleaning", "Complete deep cleaning", 150, 1250.0, true);
                serviceRepository.save(deepCleaning);
                System.out.println("✓ Created: Deep Internal Cleaning (₱1250)");
            }

            Service gpuCleaning = serviceRepository.findByName("GPU Deep Cleaning");
            if (gpuCleaning == null) {
                gpuCleaning = new Service("GPU Deep Cleaning", "Graphics card cleaning", 60, 600.0, true);
                serviceRepository.save(gpuCleaning);
                System.out.println("✓ Created: GPU Deep Cleaning (₱600)");
            }

            Service psuCleaning = serviceRepository.findByName("PSU Cleaning");
            if (psuCleaning == null) {
                psuCleaning = new Service("PSU Cleaning", "Power supply cleaning", 45, 450.0, true);
                serviceRepository.save(psuCleaning);
                System.out.println("✓ Created: PSU Cleaning (₱450)");
            }

            // Initialize Add-ons
            System.out.println("\n=== Initializing Add-ons ===");
            AddOn thermalPaste = addOnRepository.findByName("Thermal Paste Replacement");
            if (thermalPaste == null) {
                thermalPaste = new AddOn("Thermal Paste Replacement", "Apply new thermal paste", 200.0, true);
                addOnRepository.save(thermalPaste);
                System.out.println("✓ Created: Thermal Paste Replacement (₱200)");
            }

            AddOn cableManagement = addOnRepository.findByName("Cable Management");
            if (cableManagement == null) {
                cableManagement = new AddOn("Cable Management", "Organize internal cables", 50.0, true);
                addOnRepository.save(cableManagement);
                System.out.println("✓ Created: Cable Management (₱50)");
            }

            // Initialize Service-Allowed-Addon Mappings (with compatibility rules)
            System.out.println("\n=== Initializing Service-Addon Compatibility ===");
            
            // External Cleaning can have all add-ons
            addServiceAddonMapping(serviceAllowedAddonRepository, externalCleaning, thermalPaste);
            addServiceAddonMapping(serviceAllowedAddonRepository, externalCleaning, cableManagement);
            
            // Deep Cleaning CANNOT have GPU Cleaning or PSU Cleaning as add-ons (AC-10)
            // Deep Cleaning can only have thermal paste and cable management
            addServiceAddonMapping(serviceAllowedAddonRepository, deepCleaning, thermalPaste);
            addServiceAddonMapping(serviceAllowedAddonRepository, deepCleaning, cableManagement);
            System.out.println("ℹ Deep Cleaning: GPU/PSU Cleaning excluded (compatibility rule)");
            
            // GPU Cleaning can have thermal paste and cable management
            // GPU Cleaning CANNOT have Deep Cleaning as add-on (AC-10)
            addServiceAddonMapping(serviceAllowedAddonRepository, gpuCleaning, thermalPaste);
            addServiceAddonMapping(serviceAllowedAddonRepository, gpuCleaning, cableManagement);
            System.out.println("ℹ GPU Cleaning: Deep Cleaning excluded (compatibility rule)");
            
            // PSU Cleaning can have thermal paste and cable management
            addServiceAddonMapping(serviceAllowedAddonRepository, psuCleaning, thermalPaste);
            addServiceAddonMapping(serviceAllowedAddonRepository, psuCleaning, cableManagement);

            // Initialize Checklist Items (Pre-Service Only - 5 items)
            System.out.println("\n=== Initializing Pre-Service Checklist Items ===");
            List<String> checklistLabels = Arrays.asList(
                "Verify location is valid and searchable",
                "Inspect tools for service are clean and working",
                "Client available and gives consent",
                "Test device is working before beginning physical service",
                "Review service requirements with client"
            );

            for (String label : checklistLabels) {
                ChecklistItem existing = checklistItemRepository.findAll()
                    .stream()
                    .filter(item -> item.getLabel().equals(label))
                    .findFirst()
                    .orElse(null);
                
                if (existing == null) {
                    ChecklistItem item = new ChecklistItem(label, true);
                    checklistItemRepository.save(item);
                    System.out.println("✓ Created: " + label);
                }
            }

            System.out.println("\n=== Data Initialization Complete ===\n");
        };
    }

    private void addServiceAddonMapping(
            ServiceAllowedAddonRepository repository, 
            Service service, 
            AddOn addOn) {
        if (!repository.existsByServiceIdAndAddonId(service.getId(), addOn.getId())) {
            ServiceAllowedAddon mapping = new ServiceAllowedAddon(service, addOn);
            repository.save(mapping);
            System.out.println("✓ Mapped: " + service.getName() + " + " + addOn.getName());
        }
    }
}
