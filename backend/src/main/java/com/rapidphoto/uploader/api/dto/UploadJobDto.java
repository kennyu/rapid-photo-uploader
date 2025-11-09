package com.rapidphoto.uploader.api.dto;

import com.rapidphoto.uploader.domain.UploadJob;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Data Transfer Object for UploadJob entity.
 * Used for API responses in query side.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadJobDto {
    private UUID id;
    private UUID photoId;
    private UUID userId;
    private UploadJob.UploadStatus status;
    private String errorMessage;
    private Integer attemptCount;
    private Instant createdAt;
    private Instant updatedAt;
}

