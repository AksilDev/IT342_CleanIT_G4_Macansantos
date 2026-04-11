package com.G4.backend.controller;

import com.G4.backend.dto.LoginRequest;
import com.G4.backend.dto.LoginResponse;
import com.G4.backend.dto.RegisterRequest;
import com.G4.backend.service.AuthService;
<<<<<<< Updated upstream

=======
>>>>>>> Stashed changes
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

<<<<<<< Updated upstream
=======
import java.util.Map;

>>>>>>> Stashed changes
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
<<<<<<< Updated upstream
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {

        try {
            String response = authService.register(request);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
=======
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
>>>>>>> Stashed changes
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
<<<<<<< Updated upstream

        try {
            LoginResponse response = authService.login(request);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
=======
        return ResponseEntity.ok(authService.login(request));
    }

    // ── NEW: called by RoleSelection.tsx ──────────────────────────────────────

    @PostMapping("/upload-image")
    public ResponseEntity<Map<String, String>> uploadImage(
            @RequestParam("file") MultipartFile file) {
        String imageUrl = authService.uploadImage(file);
        return ResponseEntity.ok(Map.of("imageUrl", imageUrl));
    }

    @PostMapping("/oauth-complete")
    public ResponseEntity<LoginResponse> oauthComplete(
            @RequestBody OAuthCompleteRequest request) {
        return ResponseEntity.ok(authService.completeOAuthProfile(request));
    }

    @PostMapping("/oauth-check")
    public ResponseEntity<?> oauthCheck(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        return ResponseEntity.ok(authService.oauthCheck(email));
    }
>>>>>>> Stashed changes
}