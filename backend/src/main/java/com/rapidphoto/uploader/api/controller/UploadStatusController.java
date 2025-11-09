package com.rapidphoto.uploader.api.controller;

import com.rapidphoto.uploader.api.dto.UploadStatusUpdateRequest;
import com.rapidphoto.uploader.application.service.UploadStatusService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST controller for upload status management.
 * Provides endpoints for clients to update upload status and check job state.
 */
@RestController
@RequestMapping("/api/v1/uploads")
@RequiredArgsConstructor
public class UploadStatusController {

    private final UploadStatusService uploadStatusService;

    /**
     * Update upload job status.
     * Clients should call this after completing or failing an upload.
     * 
     * @param request status update request
     * @return success response
     */
    @PostMapping("/status")
    public ResponseEntity<Map<String, String>> updateStatus(
            @Valid @RequestBody UploadStatusUpdateRequest request) {
        
        uploadStatusService.updateStatus(
                request.getUploadJobId(),
                request.getStatus(),
                request.getErrorMessage()
        );
        
        return ResponseEntity.ok(Map.of(
                "message", "Status updated successfully",
                "uploadJobId", request.getUploadJobId().toString(),
                "status", request.getStatus().toString()
        ));
    }

    /**
     * Mark an upload as complete.
     * Convenience endpoint for successful uploads.
     * 
     * @param uploadJobId the upload job ID
     * @return success response
     */
    @PostMapping("/{uploadJobId}/complete")
    public ResponseEntity<Map<String, String>> markComplete(@PathVariable UUID uploadJobId) {
        uploadStatusService.markComplete(uploadJobId);
        
        return ResponseEntity.ok(Map.of(
                "message", "Upload marked as complete",
                "uploadJobId", uploadJobId.toString()
        ));
    }

    /**
     * Mark an upload as failed.
     * 
     * @param uploadJobId the upload job ID
     * @param errorMessage error description
     * @return success response
     */
    @PostMapping("/{uploadJobId}/fail")
    public ResponseEntity<Map<String, String>> markFailed(
            @PathVariable UUID uploadJobId,
            @RequestBody Map<String, String> body) {
        
        String errorMessage = body.getOrDefault("errorMessage", "Upload failed");
        uploadStatusService.markFailed(uploadJobId, errorMessage);
        
        return ResponseEntity.ok(Map.of(
                "message", "Upload marked as failed",
                "uploadJobId", uploadJobId.toString()
        ));
    }

    /**
     * Manually retry a failed upload.
     * 
     * @param uploadJobId the upload job ID
     * @return success response
     */
    @PostMapping("/{uploadJobId}/retry")
    public ResponseEntity<Map<String, String>> retryUpload(@PathVariable UUID uploadJobId) {
        if (!uploadStatusService.canRetry(uploadJobId)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Upload has exceeded maximum retry attempts"
            ));
        }
        
        uploadStatusService.retryUpload(uploadJobId);
        
        return ResponseEntity.ok(Map.of(
                "message", "Retry initiated",
                "uploadJobId", uploadJobId.toString()
        ));
    }
}

