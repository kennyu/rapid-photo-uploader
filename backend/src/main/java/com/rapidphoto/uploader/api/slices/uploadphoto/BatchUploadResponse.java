package com.rapidphoto.uploader.api.slices.uploadphoto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Response DTO for batch photo upload initiation.
 * Contains pre-signed URLs and job IDs for all files in the batch.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchUploadResponse {
    
    private Integer totalFiles;
    private Integer successfullyInitiated;
    private Integer failed;
    private List<UploadResult> uploads;
    
    /**
     * Result for a single file upload initiation.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UploadResult {
        private UUID uploadJobId;
        private UUID photoId;
        private String filename;
        private String preSignedUrl;
        private Integer expiresInSeconds;
        private Boolean success;
        private String errorMessage;
    }
}

