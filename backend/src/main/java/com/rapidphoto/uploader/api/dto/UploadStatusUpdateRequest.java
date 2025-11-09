package com.rapidphoto.uploader.api.dto;

import com.rapidphoto.uploader.domain.UploadJob;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for updating upload job status.
 * Used by clients to notify backend of upload completion or failure.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadStatusUpdateRequest {
    
    @NotNull(message = "Upload job ID is required")
    private UUID uploadJobId;
    
    @NotNull(message = "Status is required")
    private UploadJob.UploadStatus status;
    
    private String errorMessage;
}

