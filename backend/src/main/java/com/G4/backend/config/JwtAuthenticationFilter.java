package com.G4.backend.config;

import com.G4.backend.config.JwtService;
import com.G4.backend.entity.User;
import com.G4.backend.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import io.jsonwebtoken.JwtException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String requestURI = request.getRequestURI();
        
        System.out.println("DEBUG: Request URI: " + requestURI + " | Auth Header present: " + (authHeader != null));

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                final String jwt = authHeader.substring(7);
                final String email = jwtService.extractEmail(jwt);

                if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    Optional<User> optionalUser = userRepository.findByEmail(email);

                    if (optionalUser.isPresent()) {
                        User user = optionalUser.get();

                        if (jwtService.isTokenValid(jwt, user.getEmail())) {
                            String authority = "ROLE_" + user.getRole();
                            System.out.println("DEBUG: Setting authority for user " + user.getEmail() + " with role: " + user.getRole() + " -> authority: " + authority);
                            
                            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                    user,
                                    null,
                                    List.of(new SimpleGrantedAuthority(authority)));

                            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(authToken);
                            
                            System.out.println("DEBUG: Authentication set successfully. Authorities: " + authToken.getAuthorities());
                        } else {
                            System.out.println("DEBUG: Token validation failed for email: " + email);
                        }
                    } else {
                        System.out.println("DEBUG: User not found for email: " + email);
                    }
                }
            } catch (JwtException e) {
                System.out.println("DEBUG: JWT Exception: " + e.getMessage());
                // If token is expired or invalid, we simply don't set the authentication
                // context.
                // This allows public endpoints to still be accessible.
            }
        } else {
            System.out.println("DEBUG: No Bearer token found in Authorization header");
        }

        filterChain.doFilter(request, response);
    }
}
