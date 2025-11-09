package com.rapidphoto.uploader.api.slices.uploadphoto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for batch photo upload initiation.
 * Supports up to 100 concurrent uploads per session.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchUploadRequest {
    
    private UUID userId;
    
    @NotEmpty(message = "Files list cannot be empty")
    @Size(max = 100, message = "Maximum 100 files per batch")
    @Valid
    private List<PhotoMetadata> files;
    
    /**
     * Metadata for a single photo in the batch.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PhotoMetadata {
        private String filename;
        private Long fileSize;
        private String contentType;
    }
}

