package com.G4.backend.service.decorator;

/**
 * Decorator Implementation: Email Format Validation
 * 
 * Adds email format validation to the registration process.
 * Checks for valid email structure (contains @, domain, etc.)
 */
public class EmailValidationDecorator extends ValidatorDecorator {
    
    public EmailValidationDecorator(RegistrationValidator wrappedValidator) {
        super(wrappedValidator);
    }
    
    @Override
    public void validate(String email, String password, String role) {
        // First, delegate to wrapped validator
        super.validate(email, password, role);
        
        // Then add email format validation
        if (email == null || email.trim().isEmpty()) {
            throw new RuntimeException("Email is required");
        }
        
        // Simple email validation regex
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        if (!email.matches(emailRegex)) {
            throw new RuntimeException("Invalid email format");
        }
    }
}
