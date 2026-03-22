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
        
        System.out.println("OAuth authentication success received");
        
        try {
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
            Map<String, Object> attributes = oAuth2User.getAttributes();
            
            String email = (String) attributes.get("email");
            String name = (String) attributes.get("name");
            String googleId = (String) attributes.get("sub");
            
            System.out.println("OAuth user data:");
            System.out.println("Email: " + email);
            System.out.println("Name: " + name);
            System.out.println("Google ID: " + googleId);
            
            if (email == null || email.isEmpty()) {
                System.out.println("Email is null or empty, redirecting to login with error");
                response.sendRedirect("http://localhost:5173/login?error=missing_email");
                return;
            }
            
            Optional<User> existingUser = userRepository.findByEmail(email);
            
            if (existingUser.isPresent()) {
                User user = existingUser.get();
                System.out.println("Existing user found: " + user.getEmail() + ", role: " + user.getRole());
                String encodedEmail = java.net.URLEncoder.encode(email, java.nio.charset.StandardCharsets.UTF_8);
                String dashboardPath = user.getRole().equalsIgnoreCase("technician") ? "/dashboard/technician" : "/dashboard";
                response.sendRedirect("http://localhost:5173" + dashboardPath + "?oauth=success&user=" + encodedEmail + "&role=" + user.getRole());
            } else {
                System.out.println("New user, creating temporary record");
                // Create temporary user with "pending" role
                User tempUser = new User();
                tempUser.setName(name != null ? name : "Google User");
                tempUser.setEmail(email);
                tempUser.setRole("pending");
                tempUser.setContactNo("N/A");
                tempUser.setPasswordHash("OAUTH_USER_" + googleId);
                
                userRepository.save(tempUser);
                
                // Generate temporary token for security
                String tempToken = java.util.UUID.randomUUID().toString();
                
                System.out.println("Redirecting to role selection");
                // Redirect to role selection page with user data
                response.sendRedirect("http://localhost:5173/role-selection?email=" + email + "&name=" + 
                    (name != null ? name : "Google User") + "&tempToken=" + tempToken);
            }
        } catch (Exception e) {
            System.out.println("OAuth error: " + e.getMessage());
            e.printStackTrace();
            response.sendRedirect("http://localhost:5173/login?error=oauth_failed");
        }
    }
}
