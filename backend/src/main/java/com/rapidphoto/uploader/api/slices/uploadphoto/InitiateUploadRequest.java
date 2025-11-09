package com.rapidphoto.uploader.api.slices.uploadphoto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for initiating a photo upload.
 * Note: userId is NOT included here - it comes from the authenticated user.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InitiateUploadRequest {
    
    @NotBlank(message = "Filename is required")
    private String filename;
    
    @NotNull(message = "File size is required")
    @Positive(message = "File size must be positive")
    private Long fileSize;
    
    @NotBlank(message = "Content type is required")
    private String contentType;
}

