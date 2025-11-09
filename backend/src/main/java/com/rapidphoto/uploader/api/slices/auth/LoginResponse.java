package com.rapidphoto.uploader.api.slices.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Response DTO for successful login.
 * Contains JWT token and user information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;
    
    @Builder.Default
    private String tokenType = "Bearer";
    
    private UUID userId;
    private String email;
    private String fullName;
    private Long expiresIn; // milliseconds
}

