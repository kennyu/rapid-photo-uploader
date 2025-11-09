package com.rapidphoto.uploader.api.slices.uploadphoto;

import com.rapidphoto.uploader.domain.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for the UploadPhoto vertical slice.
 * Demonstrates vertical slice architecture where all related code is co-located.
 * Supports both single and batch photo uploads.
 */
@RestController
@RequestMapping("/api/v1/photos/upload")
@RequiredArgsConstructor
public class UploadPhotoController {

    private final InitiateUploadHandler handler;
    private final BatchUploadHandler batchHandler;

    /**
     * Initiate a single photo upload.
     * Returns pre-signed URL for direct client-to-S3 upload.
     */
    @PostMapping("/initiate")
    public ResponseEntity<InitiateUploadResult> initiateUpload(
            @RequestBody InitiateUploadRequest request,
            @AuthenticationPrincipal User user) {
        
        // Create command with authenticated user's ID
        InitiateUploadCommand command = new InitiateUploadCommand(
                user.getId(),
                request.getFilename(),
                request.getFileSize(),
                request.getContentType()
        );
        
        InitiateUploadResult result = handler.handle(command);
        return ResponseEntity.ok(result);
    }

    /**
     * Initiate batch photo upload (up to 100 concurrent files).
     * Returns pre-signed URLs for all files.
     * Processes uploads asynchronously for high throughput.
     */
    @PostMapping("/batch")
    public ResponseEntity<BatchUploadResponse> batchUpload(
            @Valid @RequestBody BatchUploadRequest request,
            @AuthenticationPrincipal User user) {
        
        // Set authenticated user's ID on the request
        request.setUserId(user.getId());
        
        BatchUploadResponse response = batchHandler.handleBatch(request);
        return ResponseEntity.ok(response);
    }
}

