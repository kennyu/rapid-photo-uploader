package com.rapidphoto.uploader.api.slices.uploadphoto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Result of initiating a photo upload.
 * Contains pre-signed URL and upload job ID.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitiateUploadResult {
    private UUID uploadJobId;
    private UUID photoId;
    private String preSignedUrl;
    private Integer expiresInSeconds;
}

