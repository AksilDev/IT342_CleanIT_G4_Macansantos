package com.G4.backend.service;

import com.G4.backend.dto.*;
import com.G4.backend.entity.User;
import com.G4.backend.repository.UserRepository;
import com.G4.backend.config.JwtService;
import com.G4.backend.service.decorator.*;
import com.G4.backend.service.factory.UserFactory;
import com.G4.backend.service.observer.UserEventPublisher;
import com.G4.backend.service.strategy.AuthenticationContext;
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

/**
 * AuthService — refactored with multiple design patterns:
 * 1. Factory Pattern → UserFactory.create() for user construction
 * 2. Builder Pattern → LoginResponse.Builder for response construction
 * 3. Strategy Pattern → AuthenticationContext for different auth methods
 * 4. Observer Pattern → UserEventPublisher for registration events
 * 5. Decorator Pattern → RegistrationValidator chain for validation
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserFactory userFactory;
    private final AuthenticationContext authContext;
    private final UserEventPublisher eventPublisher;
    private final RegistrationValidator registrationValidator;

    public AuthService(UserRepository userRepository, 
                       PasswordEncoder passwordEncoder, 
                       JwtService jwtService,
                       UserFactory userFactory,
                       AuthenticationContext authContext,
                       UserEventPublisher eventPublisher,
                       BaseRegistrationValidator baseValidator) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userFactory = userFactory;
        this.authContext = authContext;
        this.eventPublisher = eventPublisher;
        
        // DECORATOR PATTERN: Chain validators together
        RegistrationValidator validator = baseValidator;
        validator = new EmailValidationDecorator(validator);
        validator = new PasswordValidationDecorator(validator);
        this.registrationValidator = validator;
    }

    public String register(RegisterRequest request) {
        // Validate required fields
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new RuntimeException("Name is required. Please enter your full name.");
        }
        
        if (request.getContactNo() == null || request.getContactNo().trim().isEmpty()) {
            throw new RuntimeException("Contact number is required. Please enter your phone number.");
        }
        
        // DECORATOR PATTERN: Use validation chain
        registrationValidator.validate(
            request.getEmail(),
            request.getPassword(),
            request.getRole()
        );

        if (!request.getRole().equals("client") && !request.getRole().equals("technician")) {
            throw new RuntimeException("Invalid role selected. Please choose either 'client' or 'technician'.");
        }

        // FACTORY PATTERN: Clean single-line construction
        User user = userFactory.create(
                request.getName(),
                request.getEmail(),
                request.getPassword(),
                request.getRole(),
                request.getContactNo()
        );

        userRepository.save(user);
        
        // OBSERVER PATTERN: Notify observers of user registration
        eventPublisher.publishUserRegistered(user);

        return "Registration successful! Welcome to CleanIT. Please log in with your email and password.";
    }

    public LoginResponse login(LoginRequest request) {
        // STRATEGY PATTERN: Delegate to appropriate authentication strategy
        return authContext.authenticate("email", request);
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
            throw new RuntimeException("Failed to upload your ID image. Please try again with a smaller file (max 5MB) or different format (JPG/PNG).");
        }
    }

    public LoginResponse completeOAuthProfile(OAuthCompleteRequest request) {
        if (!request.getRole().equals("client") && !request.getRole().equals("technician")) {
            throw new RuntimeException("Invalid role selected. Please choose either 'client' or 'technician'.");
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