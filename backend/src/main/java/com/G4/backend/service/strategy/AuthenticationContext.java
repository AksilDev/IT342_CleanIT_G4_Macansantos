package com.G4.backend.service.strategy;

import com.G4.backend.dto.LoginRequest;
import com.G4.backend.dto.LoginResponse;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * Strategy Context: Authentication Service
 * 
 * Manages and delegates to the appropriate authentication strategy.
 * Uses a list of strategies and selects the right one based on the authentication type.
 */
@Component
public class AuthenticationContext {
    
    private final List<AuthenticationStrategy> strategies;
    
    public AuthenticationContext(List<AuthenticationStrategy> strategies) {
        this.strategies = strategies;
    }
    
    /**
     * Find and execute the appropriate authentication strategy
     * @param type Authentication type (e.g., "email", "google")
     * @param request Login request
     * @return LoginResponse from the selected strategy
     */
    public LoginResponse authenticate(String type, LoginRequest request) {
        AuthenticationStrategy strategy = strategies.stream()
                .filter(s -> s.supports(type))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Unsupported authentication type: " + type));
        
        return strategy.authenticate(request);
    }
}
