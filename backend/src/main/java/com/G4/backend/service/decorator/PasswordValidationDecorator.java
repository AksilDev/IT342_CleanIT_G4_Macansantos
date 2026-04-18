package com.G4.backend.service.decorator;

/**
 * Decorator Implementation: Password Strength Validation
 * 
 * Adds password strength validation to the registration process.
 * Ensures password meets minimum security requirements.
 */
public class PasswordValidationDecorator extends ValidatorDecorator {
    
    private static final int MIN_PASSWORD_LENGTH = 8;
    
    public PasswordValidationDecorator(RegistrationValidator wrappedValidator) {
        super(wrappedValidator);
    }
    
    @Override
    public void validate(String email, String password, String role) {
        // First, delegate to wrapped validator
        super.validate(email, password, role);
        
        // Then add password strength validation
        if (password == null || password.isEmpty()) {
            throw new RuntimeException("Password is required. Please create a password.");
        }
        
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new RuntimeException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters long.");
        }
        
        // Check for at least one uppercase letter
        if (!password.matches(".*[A-Z].*")) {
            throw new RuntimeException("Password must contain at least one uppercase letter (A-Z).");
        }
        
        // Check for at least one number
        if (!password.matches(".*\\d.*")) {
            throw new RuntimeException("Password must contain at least one number (0-9).");
        }
    }
}
