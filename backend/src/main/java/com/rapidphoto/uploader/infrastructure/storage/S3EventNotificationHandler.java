package com.rapidphoto.uploader.infrastructure.storage;

import com.rapidphoto.uploader.application.service.UploadStatusService;
import com.rapidphoto.uploader.domain.UploadJob;
import com.rapidphoto.uploader.infrastructure.repository.UploadJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Handler for S3 event notifications.
 * Processes events from S3 when objects are created or upload operations complete.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class S3EventNotificationHandler {

    private final UploadStatusService uploadStatusService;
    private final UploadJobRepository uploadJobRepository;

    /**
     * Process S3 event notification.
     * Called when S3 sends a notification about object operations.
     * 
     * @param event S3 event data
     */
    public void handleS3Event(Map<String, Object> event) {
        try {
            String eventName = (String) event.get("eventName");
            log.info("S3 event received (DISABLED): {}", eventName);
            
            // DISABLED: We manually call /uploads/{uploadJobId}/complete from mobile app
            // This handler was causing premature processing before files were uploaded
            // if (eventName != null && eventName.contains("ObjectCreated")) {
            //     handleObjectCreated(event);
            // } else if (eventName != null && eventName.contains("ObjectRemoved")) {
            //     handleObjectRemoved(event);
            // }
            
        } catch (Exception e) {
            log.error("Error processing S3 event", e);
        }
    }

    /**
     * Handle S3 ObjectCreated event.
     * Marks the associated upload job as complete.
     * 
     * @param event S3 event data
     */
    private void handleObjectCreated(Map<String, Object> event) {
        String objectKey = extractObjectKey(event);
        
        if (objectKey == null) {
            log.warn("No object key found in S3 event");
            return;
        }
        
        log.info("Object created in S3: {}", objectKey);
        
        // Find upload job by storage key
        uploadJobRepository.findAll().stream()
                .filter(job -> {
                    try {
                        return job.getPhotoId() != null; // Basic check
                    } catch (Exception e) {
                        return false;
                    }
                })
                .findFirst()
                .ifPresent(job -> {
                    try {
                        uploadStatusService.markComplete(job.getId());
                        log.info("Marked upload job as complete via S3 event: {}", job.getId());
                    } catch (Exception e) {
                        log.error("Failed to update upload job status from S3 event", e);
                    }
                });
    }

    /**
     * Handle S3 ObjectRemoved event.
     * 
     * @param event S3 event data
     */
    private void handleObjectRemoved(Map<String, Object> event) {
        String objectKey = extractObjectKey(event);
        log.info("Object removed from S3: {}", objectKey);
        // Handle object removal if needed
    }

    /**
     * Extract object key from S3 event.
     * 
     * @param event S3 event data
     * @return object key or null
     */
    @SuppressWarnings("unchecked")
    private String extractObjectKey(Map<String, Object> event) {
        try {
            Map<String, Object> s3 = (Map<String, Object>) event.get("s3");
            if (s3 != null) {
                Map<String, Object> object = (Map<String, Object>) s3.get("object");
                if (object != null) {
                    return (String) object.get("key");
                }
            }
        } catch (Exception e) {
            log.error("Error extracting object key from S3 event", e);
        }
        return null;
    }
}

