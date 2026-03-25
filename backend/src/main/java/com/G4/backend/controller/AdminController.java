package com.G4.backend.controller;

import com.G4.backend.entity.User;
import com.G4.backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin")
@CrossOrigin(origins = "http://localhost:5173")
public class AdminController {

    private final UserRepository userRepository;

    public AdminController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/pending-verifications")
    public ResponseEntity<?> getPendingVerifications() {
        try {
            List<String> targetRoles = Arrays.asList("client", "technician");
            List<User> pendingUsers = userRepository.findPendingVerifications(targetRoles);

            List<Map<String, Object>> response = pendingUsers.stream().map(user -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", user.getId());
                map.put("name", user.getName());
                map.put("email", user.getEmail());
                map.put("contactNo", user.getContactNo());
                map.put("role", user.getRole());
                map.put("imageUrl", user.getImageUrl());
                map.put("createdAt", user.getCreatedAt());
                map.put("verified", user.getVerified());
                return map;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to fetch pending verifications: " + e.getMessage());
        }
    }

    @PostMapping("/verify-user/{userId}")
    public ResponseEntity<?> verifyUser(@PathVariable UUID userId, @RequestBody Map<String, String> request) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String status = request.get("status");
            if ("approved".equals(status)) {
                user.setVerified(true);
                userRepository.save(user);
                return ResponseEntity.ok("User verified successfully");
            } else if ("rejected".equals(status)) {
                userRepository.delete(user);
                return ResponseEntity.ok("User rejected and removed");
            } else {
                return ResponseEntity.badRequest().body("Invalid status. Must be 'approved' or 'rejected'");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to verify user: " + e.getMessage());
        }
    }
}
