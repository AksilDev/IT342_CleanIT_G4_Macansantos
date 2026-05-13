package com.G4.backend.features.auth;

import com.G4.backend.features.users.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class UserFactory {

    private final PasswordEncoder passwordEncoder;

    public UserFactory(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public User create(String name, String email, String password, String role, String contactNo, String imageUrl) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        user.setContactNo(contactNo);
        user.setImageUrl(imageUrl);
        return user;
    }
}
