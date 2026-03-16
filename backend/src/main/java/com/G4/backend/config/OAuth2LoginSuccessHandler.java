package com.G4.backend.config;

import com.G4.backend.entity.User;
import com.G4.backend.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        
        try {
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
            Map<String, Object> attributes = oAuth2User.getAttributes();
            
            String email = (String) attributes.get("email");
            String name = (String) attributes.get("name");
            String googleId = (String) attributes.get("sub");
            
            if (email == null || email.isEmpty()) {
                response.sendRedirect("http://localhost:5173/login?error=missing_email");
                return;
            }
            
            Optional<User> existingUser = userRepository.findByEmail(email);
            
            if (existingUser.isPresent()) {
                User user = existingUser.get();
                response.sendRedirect("http://localhost:5173/dashboard?oauth=success&user=" + email + "&role=" + user.getRole());
            } else {
                User newUser = new User();
                newUser.setName(name != null ? name : "Google User");
                newUser.setEmail(email);
                newUser.setRole("client");
                newUser.setContactNo("N/A");
                newUser.setPasswordHash("OAUTH_USER_" + googleId);
                
                userRepository.save(newUser);
                response.sendRedirect("http://localhost:5173/dashboard?oauth=success&user=" + email + "&role=client");
            }
        } catch (Exception e) {
            response.sendRedirect("http://localhost:5173/login?error=oauth_failed");
        }
    }
}
