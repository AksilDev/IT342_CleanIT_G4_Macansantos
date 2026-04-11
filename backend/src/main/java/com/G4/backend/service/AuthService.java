package com.G4.backend.service;

import com.G4.backend.dto.LoginRequest;
import com.G4.backend.dto.RegisterRequest;
import com.G4.backend.entity.User;
import com.G4.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

<<<<<<< Updated upstream
=======
import com.G4.backend.dto.OAuthCompleteRequest;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * AuthService — refactored to use:
 * 1. Factory Pattern → UserFactory.create() replaces raw setter block
 * 2. Builder Pattern → LoginResponse.Builder replaces raw setter block
 */
>>>>>>> Stashed changes
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public String register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        if (!request.getRole().equals("client") && !request.getRole().equals("technician")) {
            throw new RuntimeException("Invalid role");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setContactNo(request.getContactNo());
        user.setRole(request.getRole());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        userRepository.save(user);

        return "User registered successfully";
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid password");
        }

        LoginResponse response = new LoginResponse();
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        response.setContactNo(user.getContactNo());
        response.setMessage("Login successful");

        return response;
    }

    public String uploadImage(MultipartFile file) {
        try {
            String uploadDir = "uploads/";
            Files.createDirectories(Paths.get(uploadDir));

            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path filePath = Paths.get(uploadDir + filename);
            Files.write(filePath, file.getBytes());

            // Return a URL the frontend can use
            return "http://localhost:8080/uploads/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload image: " + e.getMessage());
        }
    }

    public LoginResponse completeOAuthProfile(OAuthCompleteRequest request) {
        if (!request.getRole().equals("client") && !request.getRole().equals("technician")) {
            throw new RuntimeException("Invalid role");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(request.getEmail());
                    newUser.setName(request.getName() != null ? request.getName() : "Google User");
                    // Generating a random secure password for OAuth users since they sign in via Google
                    newUser.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
                    return newUser;
                });

        // Update the user with real info
        user.setRole(request.getRole());
        user.setContactNo(request.getContactNo());
        user.setImageUrl(request.getImageUrl());
        user.setVerified(false); // admin must verify

        userRepository.save(user);

        String token = jwtService.generateToken(user.getEmail(), user.getRole());

        return new LoginResponse.Builder()
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .contactNo(user.getContactNo())
                .verified(user.getVerified())
                .message("Profile complete. Awaiting verification.")
                .token(token)
                .build();
    }

    public java.util.Map<String, Object> oauthCheck(String email) {
        java.util.Optional<User> user = userRepository.findByEmail(email);
        if (user.isPresent()) {
            String token = jwtService.generateToken(user.get().getEmail(), user.get().getRole());
            return java.util.Map.of(
                "exists", true,
                "role", user.get().getRole(),
                "token", token
            );
        }
        return java.util.Map.of("exists", false);
    }
}