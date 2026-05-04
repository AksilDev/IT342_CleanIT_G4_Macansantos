package com.G4.backend.config;

import com.G4.backend.entity.User;
import com.G4.backend.entity.Service;
import com.G4.backend.entity.AddOn;
import com.G4.backend.entity.ServiceAllowedAddon;
import com.G4.backend.entity.ChecklistItem;
import com.G4.backend.entity.BookingChecklist;
import com.G4.backend.repository.UserRepository;
import com.G4.backend.repository.ServiceRepository;
import com.G4.backend.repository.AddOnRepository;
import com.G4.backend.repository.ServiceAllowedAddonRepository;
import com.G4.backend.repository.ChecklistItemRepository;
import com.G4.backend.repository.BookingChecklistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Configuration
public class DataInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

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
            ChecklistItemRepository checklistItemRepository,
            BookingChecklistRepository bookingChecklistRepository) {
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
                logger.info("✓ Super admin account created successfully!");
                logger.info("  Email: {}", adminConfig.getAdminEmail());
                logger.info("  Password: {}", adminConfig.getAdminPassword());
            } else {
                logger.info("✓ Super admin account already exists");
            }

            // Initialize Services
            logger.info("\n=== Initializing Services ===");
            Service externalCleaning = serviceRepository.findByName("Standard External Cleaning");
            if (externalCleaning == null) {
                externalCleaning = new Service("Standard External Cleaning", "Complete external cleaning service", 90, 200.0, true);
                serviceRepository.save(externalCleaning);
                logger.info("✓ Created: Standard External Cleaning (₱200)");
            }

            Service deepCleaning = serviceRepository.findByName("Deep Internal Cleaning");
            if (deepCleaning == null) {
                deepCleaning = new Service("Deep Internal Cleaning", "Complete deep cleaning", 150, 1250.0, true);
                serviceRepository.save(deepCleaning);
                logger.info("✓ Created: Deep Internal Cleaning (₱1250)");
            }

            Service gpuCleaning = serviceRepository.findByName("GPU Deep Cleaning");
            if (gpuCleaning == null) {
                gpuCleaning = new Service("GPU Deep Cleaning", "Graphics card cleaning", 60, 600.0, true);
                serviceRepository.save(gpuCleaning);
                logger.info("✓ Created: GPU Deep Cleaning (₱600)");
            }

            Service psuCleaning = serviceRepository.findByName("PSU Cleaning");
            if (psuCleaning == null) {
                psuCleaning = new Service("PSU Cleaning", "Power supply cleaning", 45, 450.0, true);
                serviceRepository.save(psuCleaning);
                logger.info("✓ Created: PSU Cleaning (₱450)");
            }

            // Initialize Add-ons
            logger.info("\n=== Initializing Add-ons ===");
            AddOn thermalPaste = addOnRepository.findByName("Thermal Paste Replacement");
            if (thermalPaste == null) {
                thermalPaste = new AddOn("Thermal Paste Replacement", "Apply new thermal paste", 200.0, true);
                addOnRepository.save(thermalPaste);
                logger.info("✓ Created: Thermal Paste Replacement (₱200)");
            }

            AddOn cableManagement = addOnRepository.findByName("Cable Management");
            if (cableManagement == null) {
                cableManagement = new AddOn("Cable Management", "Organize internal cables", 50.0, true);
                addOnRepository.save(cableManagement);
                logger.info("✓ Created: Cable Management (₱50)");
            }

            // Create GPU Deep Cleaning and PSU Cleaning as add-ons (for Standard External Cleaning)
            AddOn gpuDeepCleaningAddon = addOnRepository.findByName("GPU Deep Cleaning (Add-on)");
            if (gpuDeepCleaningAddon == null) {
                gpuDeepCleaningAddon = new AddOn("GPU Deep Cleaning (Add-on)", "Graphics card deep cleaning service", 600.0, true);
                addOnRepository.save(gpuDeepCleaningAddon);
                logger.info("✓ Created: GPU Deep Cleaning Add-on (₱600)");
            }

            AddOn psuCleaningAddon = addOnRepository.findByName("PSU Cleaning (Add-on)");
            if (psuCleaningAddon == null) {
                psuCleaningAddon = new AddOn("PSU Cleaning (Add-on)", "Power supply cleaning service", 450.0, true);
                addOnRepository.save(psuCleaningAddon);
                logger.info("✓ Created: PSU Cleaning Add-on (₱450)");
            }

            // Initialize Service-Allowed-Addon Mappings (with compatibility rules)
            logger.info("\n=== Initializing Service-Addon Compatibility ===");
            
            // CLEANUP: Remove all existing mappings for GPU and PSU services to ensure clean state
            logger.info("Cleaning up old mappings for GPU Deep Cleaning and PSU Cleaning...");
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
            logger.info("✓ Old mappings cleaned up");
            
            // Standard External Cleaning: GPU Deep cleaning, PSU cleaning, Thermal Paste, Cable Management
            addServiceAddonMapping(serviceAllowedAddonRepository, externalCleaning, gpuDeepCleaningAddon);
            addServiceAddonMapping(serviceAllowedAddonRepository, externalCleaning, psuCleaningAddon);
            addServiceAddonMapping(serviceAllowedAddonRepository, externalCleaning, thermalPaste);
            addServiceAddonMapping(serviceAllowedAddonRepository, externalCleaning, cableManagement);
            logger.info("✓ Standard External Cleaning: 4 add-ons available");
            
            // Deep Internal Cleaning: Thermal Paste, Cable Management only
            addServiceAddonMapping(serviceAllowedAddonRepository, deepCleaning, thermalPaste);
            addServiceAddonMapping(serviceAllowedAddonRepository, deepCleaning, cableManagement);
            logger.info("✓ Deep Internal Cleaning: 2 add-ons available");
            
            // GPU Deep Cleaning: Thermal Paste ONLY (Cable Management removed)
            addServiceAddonMapping(serviceAllowedAddonRepository, gpuCleaning, thermalPaste);
            logger.info("✓ GPU Deep Cleaning: 1 add-on available (Thermal Paste only)");
            
            // PSU Cleaning: NO add-ons at all
            logger.info("✓ PSU Cleaning: 0 add-ons available (none)");

            // Initialize Checklist Items (Pre-Service Only - 5 items)
            logger.info("\n=== Initializing Pre-Service Checklist Items ===");
            
            // Clean up old booking checklist entries first (to avoid foreign key constraint)
            List<BookingChecklist> oldBookingChecklists = bookingChecklistRepository.findAll();
            if (!oldBookingChecklists.isEmpty()) {
                bookingChecklistRepository.deleteAll(oldBookingChecklists);
                logger.info("✓ Cleaned up {} old booking checklist entries", oldBookingChecklists.size());
            }
            
            // Now clean up old checklist items
            List<ChecklistItem> oldItems = checklistItemRepository.findAll();
            if (!oldItems.isEmpty()) {
                checklistItemRepository.deleteAll(oldItems);
                logger.info("✓ Cleaned up {} old checklist items", oldItems.size());
            }
            
            List<String> checklistLabels = Arrays.asList(
                "Verify location is valid and searchable",
                "Inspect tools for service are clean and working",
                "Client available and gives consent",
                "Inspect unit for physical damages",
                "Take a photo of unit before service starts"
            );

            for (String label : checklistLabels) {
                ChecklistItem item = new ChecklistItem(label, true);
                checklistItemRepository.save(item);
                logger.info("✓ Created: {}", label);
            }

            logger.info("\n=== Data Initialization Complete ===\n");
        };
    }

    private void addServiceAddonMapping(
            ServiceAllowedAddonRepository repository, 
            Service service, 
            AddOn addOn) {
        if (!repository.existsByServiceIdAndAddonId(service.getId(), addOn.getId())) {
            ServiceAllowedAddon mapping = new ServiceAllowedAddon(service, addOn);
            repository.save(mapping);
            logger.info("✓ Mapped: {} + {}", service.getName(), addOn.getName());
        }
    }
}
