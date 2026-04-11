package com.G4.backend.service.decorator;

/**
 * Decorator Base Class: Registration Validator Decorator
 * 
 * Abstract decorator that wraps a RegistrationValidator and delegates to it.
 * Concrete decorators extend this class to add specific validation logic.
 */
public abstract class ValidatorDecorator implements RegistrationValidator {
    
    protected final RegistrationValidator wrappedValidator;
    
    public ValidatorDecorator(RegistrationValidator wrappedValidator) {
        this.wrappedValidator = wrappedValidator;
    }
    
    @Override
    public void validate(String email, String password, String role) {
        // Delegate to wrapped validator
        wrappedValidator.validate(email, password, role);
    }
}
