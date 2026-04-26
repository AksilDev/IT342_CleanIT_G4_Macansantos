package com.G4.backend.controller;

import com.G4.backend.entity.AddOn;
import com.G4.backend.entity.Service;
import com.G4.backend.repository.AddOnRepository;
import com.G4.backend.repository.ServiceAllowedAddonRepository;
import com.G4.backend.repository.ServiceRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/services")
@CrossOrigin(origins = "http://localhost:5173")
public class ServiceController {

    private final ServiceRepository serviceRepository;
    private final AddOnRepository addOnRepository;
    private final ServiceAllowedAddonRepository serviceAllowedAddonRepository;

    public ServiceController(ServiceRepository serviceRepository, 
                            AddOnRepository addOnRepository,
                            ServiceAllowedAddonRepository serviceAllowedAddonRepository) {
        this.serviceRepository = serviceRepository;
        this.addOnRepository = addOnRepository;
        this.serviceAllowedAddonRepository = serviceAllowedAddonRepository;
    }

    /**
     * Get all active services
     */
    @GetMapping
    public ResponseEntity<?> getAllServices() {
        try {
            List<Service> services = serviceRepository.findByIsActiveTrue();
            List<Map<String, Object>> response = new ArrayList<>();
            
            for (Service service : services) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", service.getId());
                map.put("name", service.getName());
                map.put("description", service.getDescription());
                map.put("durationMinutes", service.getDurationMinutes());
                map.put("basePrice", service.getBasePrice());
                map.put("isActive", service.getIsActive());
                response.add(map);
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to fetch services",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Get compatible add-ons for a specific service
     */
    @GetMapping("/{serviceId}/addons")
    public ResponseEntity<?> getCompatibleAddOns(@PathVariable UUID serviceId) {
        try {
            // Verify service exists
            serviceRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Service not found"));
            
            List<AddOn> compatibleAddOns = serviceAllowedAddonRepository.findAddOnsByServiceId(serviceId);
            List<Map<String, Object>> response = new ArrayList<>();
            
            for (AddOn addOn : compatibleAddOns) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", addOn.getId());
                map.put("name", addOn.getName());
                map.put("description", addOn.getDescription());
                map.put("price", addOn.getPrice());
                map.put("isActive", addOn.getIsActive());
                response.add(map);
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to fetch compatible add-ons",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Get all active add-ons
     */
    @GetMapping("/addons")
    public ResponseEntity<?> getAllAddOns() {
        try {
            List<AddOn> addOns = addOnRepository.findByIsActiveTrue();
            List<Map<String, Object>> response = new ArrayList<>();
            
            for (AddOn addOn : addOns) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", addOn.getId());
                map.put("name", addOn.getName());
                map.put("description", addOn.getDescription());
                map.put("price", addOn.getPrice());
                map.put("isActive", addOn.getIsActive());
                response.add(map);
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to fetch add-ons",
                "message", e.getMessage()
            ));
        }
    }
}
