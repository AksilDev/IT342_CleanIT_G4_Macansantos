package com.G4.backend.service.strategy;

import com.G4.backend.dto.LoginRequest;
import com.G4.backend.dto.LoginResponse;

/**
 * PATTERN: Strategy (Behavioral)
 * 
 * PROBLEM SOLVED:
 * Authentication can happen through multiple methods (email/password, Google OAuth, etc.)
 * Without Strategy pattern, AuthService would have complex if-else chains to handle
 * each authentication type, making it hard to add new methods.
 * 
 * HOW IT WORKS:
 * Define a common interface for all authentication strategies.
 * Each authentication method implements this interface.
 * Context (AuthService) delegates to the appropriate strategy.
 * 
 * REAL-WORLD EXAMPLE:
 * Spring Security uses strategy pattern for different authentication providers
 * (DaoAuthenticationProvider, OAuth2AuthenticationProvider, etc.)
 * 
 * USE CASE IN THIS PROJECT:
 * EmailPasswordAuthStrategy and GoogleOAuthAuthStrategy implement AuthenticationStrategy.
 * AuthService can switch between them dynamically based on login type.
 */
public interface AuthenticationStrategy {
    
    /**
     * Authenticate user using specific method
     * @param request Login request containing credentials
     * @return LoginResponse with user details and token
     */
    LoginResponse authenticate(LoginRequest request);
    
    /**
     * Check if this strategy supports the given authentication type
     * @param type Authentication type (e.g., "email", "google")
     * @return true if this strategy handles this type
     */
    boolean supports(String type);
}
