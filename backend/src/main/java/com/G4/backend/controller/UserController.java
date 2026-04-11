package com.G4.backend.controller;

import com.G4.backend.entity.User;
import com.G4.backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/user")
@CrossOrigin(origins = "http://localhost:5173")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/profile/{email}")
    public ResponseEntity<?> getUserProfile(@PathVariable String email) {
        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("name", user.getName());
            response.put("email", user.getEmail());
            response.put("contactNo", user.getContactNo());
            response.put("role", user.getRole());
            response.put("verified", user.getVerified());
            response.put("imageUrl", user.getImageUrl());
            response.put("createdAt", user.getCreatedAt());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to fetch user profile: " + e.getMessage());
        }
    }

    @GetMapping("/technicians/verified")
    public ResponseEntity<?> getVerifiedTechnicians() {
        try {
            List<User> technicians = userRepository.findTop5VerifiedTechnicians();
            List<Map<String, Object>> response = technicians.stream().map(tech -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", tech.getId());
                map.put("name", tech.getName());
                map.put("email", tech.getEmail());
                map.put("contactNo", tech.getContactNo());
                map.put("imageUrl", tech.getImageUrl());
                map.put("available", true);
                map.put("rating", 4.5 + Math.random() * 0.5); // Random rating between 4.5-5.0
                map.put("reviews", (int)(50 + Math.random() * 100)); // Random reviews 50-150
                return map;
            }).collect(Collectors.toList());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to fetch technicians: " + e.getMessage());
        }
    }

    @GetMapping("/technicians/all-verified")
    public ResponseEntity<?> getAllVerifiedTechnicians() {
        try {
            List<User> technicians = userRepository.findAllVerifiedTechnicians();
            List<Map<String, Object>> response = technicians.stream().map(tech -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", tech.getId());
                map.put("name", tech.getName());
                map.put("email", tech.getEmail());
                map.put("contactNo", tech.getContactNo());
                map.put("imageUrl", tech.getImageUrl());
                map.put("verified", tech.getVerified());
                map.put("createdAt", tech.getCreatedAt());
                return map;
            }).collect(Collectors.toList());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to fetch technicians: " + e.getMessage());
        }
    }
}
