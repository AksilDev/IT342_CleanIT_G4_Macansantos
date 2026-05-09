package com.G4.backend.features.auth;

import com.G4.backend.features.auth.LoginRequest;
import com.G4.backend.features.auth.LoginResponse;
import com.G4.backend.features.users.User;
import com.G4.backend.features.users.UserRepository;
import com.G4.backend.shared.config.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Strategy Implementation: Email/Password Authentication
 * 
 * Handles traditional email and password authentication flow.
 */
@Component
public class EmailPasswordAuthStrategy implements AuthenticationStrategy {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    
    public EmailPasswordAuthStrategy(UserRepository userRepository,
                                      PasswordEncoder passwordEncoder,
                                      JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }
    
    @Override
    public LoginResponse authenticate(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("No account found with this email. Please check your email or register a new account."));
        
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Incorrect password. Please try again or reset your password.");
        }
        
        String role = normalizeRole(user.getRole());
        String token = jwtService.generateToken(user.getEmail(), role);
        
        return new LoginResponse.Builder()
                .id(user.getId().toString())
                .name(user.getName())
                .email(user.getEmail())
                .role(role)
                .contactNo(user.getContactNo())
                .verified(user.getVerified())
                .message("Login successful")
                .token(token)
                .build();
    }
    
    @Override
    public boolean supports(String type) {
        return "email".equalsIgnoreCase(type) || type == null;
    }

    private String normalizeRole(String role) {
        return role == null ? "" : role.trim().toLowerCase().replaceFirst("^role_", "");
    }
}
