package com.rapidphoto.uploader.api.dto;

import com.rapidphoto.uploader.domain.Photo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Data Transfer Object for Photo entity.
 * Used for API responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhotoDto {
    private UUID id;
    private String filename;
    private Long fileSize;
    private UUID userId;
    private Photo.PhotoStatus status;
    private String contentType;
    private Set<String> tags;
    private Instant createdAt;
    private Instant updatedAt;
    
    // Pre-signed URLs for accessing photos (generated on-demand)
    private String thumbnailUrl;  // URL to download thumbnail
    private String downloadUrl;   // URL to download full photo
}

