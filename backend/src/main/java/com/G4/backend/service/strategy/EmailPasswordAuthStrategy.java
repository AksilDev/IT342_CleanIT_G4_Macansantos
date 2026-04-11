package com.G4.backend.service.strategy;

import com.G4.backend.dto.LoginRequest;
import com.G4.backend.dto.LoginResponse;
import com.G4.backend.entity.User;
import com.G4.backend.repository.UserRepository;
import com.G4.backend.service.JwtService;
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
                .verified(user.getVerified())
                .message("Login successful")
                .token(token)
                .build();
    }
    
    @Override
    public boolean supports(String type) {
        return "email".equalsIgnoreCase(type) || type == null;
    }
}
