package com.G4.backend.service;

import com.G4.backend.dto.LoginRequest;
import com.G4.backend.dto.RegisterRequest;
import com.G4.backend.entity.User;
import com.G4.backend.repository.UserRepository;
import java.util.Map;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.authenticationManager = authenticationManager;
	}

	public void register(RegisterRequest request) {
		if (request == null) {
			throw new IllegalArgumentException("Request is required");
		}
		if (request.getUsername() == null || request.getUsername().isBlank()) {
			throw new IllegalArgumentException("Username is required");
		}
		if (request.getPassword() == null || request.getPassword().isBlank()) {
			throw new IllegalArgumentException("Password is required");
		}
		if (userRepository.existsByUsername(request.getUsername())) {
			throw new IllegalArgumentException("Username already exists");
		}

		User user = new User();
		user.setUsername(request.getUsername());
		user.setPassword(passwordEncoder.encode(request.getPassword()));
		user.setRole("USER");

		userRepository.save(user);
	}

	public Map<String, Object> login(LoginRequest request) {
		if (request == null) {
			throw new IllegalArgumentException("Request is required");
		}
		Authentication auth = authenticationManager.authenticate(
			new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
		);

		return Map.of(
			"message", "Logged in",
			"username", auth.getName()
		);
	}
}
