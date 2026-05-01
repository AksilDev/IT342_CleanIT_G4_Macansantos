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

            // Create GPU Deep Cleaning and PSU Cleaning as add-ons (for Standard External Cleaning)
            AddOn gpuDeepCleaningAddon = addOnRepository.findByName("GPU Deep Cleaning (Add-on)");
            if (gpuDeepCleaningAddon == null) {
                gpuDeepCleaningAddon = new AddOn("GPU Deep Cleaning (Add-on)", "Graphics card deep cleaning service", 600.0, true);
                addOnRepository.save(gpuDeepCleaningAddon);
                System.out.println("✓ Created: GPU Deep Cleaning Add-on (₱600)");
            }

            AddOn psuCleaningAddon = addOnRepository.findByName("PSU Cleaning (Add-on)");
            if (psuCleaningAddon == null) {
                psuCleaningAddon = new AddOn("PSU Cleaning (Add-on)", "Power supply cleaning service", 450.0, true);
                addOnRepository.save(psuCleaningAddon);
                System.out.println("✓ Created: PSU Cleaning Add-on (₱450)");
            }

            // Initialize Service-Allowed-Addon Mappings (with compatibility rules)
            System.out.println("\n=== Initializing Service-Addon Compatibility ===");
            
            // CLEANUP: Remove all existing mappings for GPU and PSU services to ensure clean state
            System.out.println("Cleaning up old mappings for GPU Deep Cleaning and PSU Cleaning...");
            final Service gpuCleaningFinal = gpuCleaning;
            final Service psuCleaningFinal = psuCleaning;
            serviceAllowedAddonRepository.deleteAll(
                serviceAllowedAddonRepository.findAll().stream()
                    .filter(mapping -> 
                        mapping.getService().getId().equals(gpuCleaningFinal.getId()) ||
                        mapping.getService().getId().equals(psuCleaningFinal.getId())
                    )
                    .toList()
            );
            System.out.println("✓ Old mappings cleaned up");
            
            // Standard External Cleaning: GPU Deep cleaning, PSU cleaning, Thermal Paste, Cable Management
            addServiceAddonMapping(serviceAllowedAddonRepository, externalCleaning, gpuDeepCleaningAddon);
            addServiceAddonMapping(serviceAllowedAddonRepository, externalCleaning, psuCleaningAddon);
            addServiceAddonMapping(serviceAllowedAddonRepository, externalCleaning, thermalPaste);
            addServiceAddonMapping(serviceAllowedAddonRepository, externalCleaning, cableManagement);
            System.out.println("✓ Standard External Cleaning: 4 add-ons available");
            
            // Deep Internal Cleaning: Thermal Paste, Cable Management only
            addServiceAddonMapping(serviceAllowedAddonRepository, deepCleaning, thermalPaste);
            addServiceAddonMapping(serviceAllowedAddonRepository, deepCleaning, cableManagement);
            System.out.println("✓ Deep Internal Cleaning: 2 add-ons available");
            
            // GPU Deep Cleaning: Thermal Paste ONLY (Cable Management removed)
            addServiceAddonMapping(serviceAllowedAddonRepository, gpuCleaning, thermalPaste);
            System.out.println("✓ GPU Deep Cleaning: 1 add-on available (Thermal Paste only)");
            
            // PSU Cleaning: NO add-ons at all
            System.out.println("✓ PSU Cleaning: 0 add-ons available (none)");

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
