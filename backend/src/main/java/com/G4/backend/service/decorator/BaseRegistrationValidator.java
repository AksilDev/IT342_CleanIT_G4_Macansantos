package com.G4.backend.service.decorator;

import com.G4.backend.repository.UserRepository;
import org.springframework.stereotype.Component;

/**
 * Base Validator Implementation: Checks for duplicate email
 */
@Component
public class BaseRegistrationValidator implements RegistrationValidator {
    
    private final UserRepository userRepository;
    
    public BaseRegistrationValidator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    @Override
    public void validate(String email, String password, String role) {
        // Check for duplicate email
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists");
        }
    }
}
