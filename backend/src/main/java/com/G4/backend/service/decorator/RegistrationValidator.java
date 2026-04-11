package com.G4.backend.service.decorator;

/**
 * PATTERN: Decorator (Structural)
 * 
 * PROBLEM SOLVED:
 * Registration requests need multiple validation steps (email format, password strength,
 * duplicate check, role validation). Without Decorator, we'd have one massive validation
 * method or duplicate validation code across multiple places.
 * 
 * HOW IT WORKS:
 * Define a component interface (RegistrationValidator).
 * Create concrete validator implementations.
 * Decorators wrap validators to add additional validation layers.
 * Each decorator adds its own validation before/after delegating to the wrapped validator.
 * 
 * REAL-WORLD EXAMPLE:
 * Java I/O streams use Decorator extensively (BufferedReader wraps InputStreamReader
 * which wraps FileInputStream). Each wrapper adds functionality.
 * 
 * USE CASE IN THIS PROJECT:
 * EmailValidationDecorator, PasswordValidationDecorator, and RoleValidationDecorator
 * wrap base validator to provide layered, composable validation logic.
 */
public interface RegistrationValidator {
    
    /**
     * Validate the registration request
     * @param email User email
     * @param password User password
     * @param role User role
     * @throws RuntimeException if validation fails
     */
    void validate(String email, String password, String role);
}
