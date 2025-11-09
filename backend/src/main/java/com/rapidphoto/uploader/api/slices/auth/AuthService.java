package com.rapidphoto.uploader.api.slices.auth;

import com.rapidphoto.uploader.domain.User;
import com.rapidphoto.uploader.infrastructure.repository.UserRepository;
import com.rapidphoto.uploader.infrastructure.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for handling authentication operations.
 * Manages user login, registration, and JWT token generation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    
    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;

    /**
     * Authenticate user and generate JWT token.
     * @param request login credentials
     * @return login response with JWT token
     * @throws RuntimeException if authentication fails
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());
        
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));
        
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Invalid password attempt for email: {}", request.getEmail());
            throw new RuntimeException("Invalid email or password");
        }
        
        String token = jwtService.generateToken(user.getId(), user.getEmail());
        
        log.info("Successfully logged in user: {}", user.getEmail());
        
        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .expiresIn(jwtExpirationMs)
                .build();
    }

    /**
     * Register a new user.
     * @param request registration details
     * @return login response with JWT token
     * @throws RuntimeException if email already exists
     */
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        log.info("Registration attempt for email: {}", request.getEmail());
        
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }
        
        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .build();
        
        user = userRepository.save(user);
        
        String token = jwtService.generateToken(user.getId(), user.getEmail());
        
        log.info("Successfully registered user: {}", user.getEmail());
        
        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .expiresIn(jwtExpirationMs)
                .build();
    }

    /**
     * Create a mock user for development/testing.
     * @param email email address
     * @param password plain password
     * @param fullName full name
     * @return created user
     */
    @Transactional
    public User createMockUser(String email, String password, String fullName) {
        if (userRepository.existsByEmail(email)) {
            return userRepository.findByEmail(email).orElseThrow();
        }
        
        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .fullName(fullName)
                .build();
        
        return userRepository.save(user);
    }
}

