package com.G4.backend.controller;

import com.G4.backend.dto.LoginRequest;
import com.G4.backend.dto.LoginResponse;
import com.G4.backend.dto.OAuthCompleteRequest;
import com.G4.backend.dto.RegisterRequest;
import com.G4.backend.entity.User;
import com.G4.backend.repository.UserRepository;
import com.G4.backend.service.AuthService;
import com.G4.backend.service.SupabaseStorageService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final SupabaseStorageService storageService;

    public AuthController(AuthService authService, UserRepository userRepository, SupabaseStorageService storageService) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.storageService = storageService;
    }

    @PostMapping("/upload-image")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            // Generate temporary user ID for the filename
            String tempUserId = "temp_" + UUID.randomUUID().toString();
            String imageUrl = storageService.uploadFile(file, tempUserId);
            
            Map<String, String> response = new HashMap<>();
            response.put("imageUrl", imageUrl);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Failed to upload image: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {

        try {
            String response = authService.register(request);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        System.out.println("Login request received:");
        System.out.println("Email: " + request.getEmail());
        System.out.println("Password: " + (request.getPassword() != null ? "provided" : "null"));
        
        try {
            LoginResponse response = authService.login(request);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.out.println("Login error: " + e.getMessage());
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/oauth-complete")
    public ResponseEntity<?> completeOAuthRegistration(@RequestBody OAuthCompleteRequest request) {
        try {
            // Validate request
            if (request.getEmail() == null || request.getRole() == null) {
                return ResponseEntity.badRequest().body("Email and role are required");
            }

            // Validate role
            if (!request.getRole().equals("client") && !request.getRole().equals("technician")) {
                return ResponseEntity.badRequest().body("Invalid role. Must be 'client' or 'technician'");
            }

            // Find user with pending role
            User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

            // Update user role from pending to selected role
            if (!user.getRole().equals("pending")) {
                return ResponseEntity.badRequest().body("User registration already completed");
            }

            user.setRole(request.getRole());
            userRepository.save(user);

            // Return user data for frontend
            LoginResponse response = new LoginResponse();
            response.setName(user.getName());
            response.setEmail(user.getEmail());
            response.setRole(user.getRole());
            response.setContactNo(user.getContactNo());
            response.setMessage("Registration completed successfully");
            
            return ResponseEntity.ok().body(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to complete registration: " + e.getMessage());
        }
    }
}