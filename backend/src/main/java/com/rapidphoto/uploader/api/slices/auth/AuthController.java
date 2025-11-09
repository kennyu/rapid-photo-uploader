package com.rapidphoto.uploader.api.slices.auth;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication endpoints.
 * Handles login, registration, and mock authentication.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Login endpoint.
     * @param request login credentials
     * @return JWT token and user information
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Registration endpoint.
     * @param request registration details
     * @return JWT token and user information
     */
    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        LoginResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Mock login endpoint for development.
     * Creates a test user and logs in.
     * WARNING: Should be disabled in production!
     */
    @PostMapping("/mock-login")
    public ResponseEntity<LoginResponse> mockLogin() {
        // Create mock user
        authService.createMockUser("test@example.com", "password123", "Test User");
        
        // Login with mock credentials
        LoginRequest request = new LoginRequest("test@example.com", "password123");
        LoginResponse response = authService.login(request);
        
        return ResponseEntity.ok(response);
    }
}

