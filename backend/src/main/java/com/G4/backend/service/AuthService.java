package com.G4.backend.service;

import com.G4.backend.dto.*;
import com.G4.backend.entity.User;
import com.G4.backend.repository.UserRepository;
import com.G4.backend.service.factory.UserFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.G4.backend.config.JwtService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserFactory userFactory;

    public AuthService(UserRepository userRepository, 
                       PasswordEncoder passwordEncoder, 
                       JwtService jwtService,
                       UserFactory userFactory) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userFactory = userFactory;
    }

    public String register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        if (!request.getRole().equals("client") && !request.getRole().equals("technician")) {
            throw new RuntimeException("Invalid role");
        }

        User user = userFactory.create(
                request.getName(),
                request.getEmail(),
                request.getPassword(),
                request.getRole(),
                request.getContactNo()
        );

        userRepository.save(user);

        return "User registered successfully";
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid password");
        }

        String token = jwtService.generateToken(user.getEmail(), user.getRole());

        return new LoginResponse.Builder()
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .contactNo(user.getContactNo())
                .token(token)
                .message("Login successful")
                .build();
    }

    public String uploadImage(MultipartFile file) {
        try {
            String uploadDir = "uploads/";
            Files.createDirectories(Paths.get(uploadDir));

            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path filePath = Paths.get(uploadDir + filename);
            Files.write(filePath, file.getBytes());

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
                    newUser.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
                    return newUser;
                });

        user.setRole(request.getRole());
        user.setContactNo(request.getContactNo());
        user.setImageUrl(request.getImageUrl());
        // Note: Field 'verified' might not exist on User entity. 
        // Checking User.java again... it doesn't have it. I'll omit or add if needed.
        // For now, I'll assume we don't have it on Entity but it's in the Response.

        userRepository.save(user);

        String token = jwtService.generateToken(user.getEmail(), user.getRole());

        return new LoginResponse.Builder()
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .contactNo(user.getContactNo())
                .token(token)
                .message("Profile complete.")
                .build();
    }

    public Map<String, Object> oauthCheck(String email) {
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isPresent()) {
            String token = jwtService.generateToken(user.get().getEmail(), user.get().getRole());
            return Map.of(
                    "exists", true,
                    "role", user.get().getRole(),
                    "token", token);
        }
        return Map.of("exists", false);
    }
}