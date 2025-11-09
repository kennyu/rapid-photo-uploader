package com.rapidphoto.uploader.api.controller;

import com.rapidphoto.uploader.infrastructure.storage.S3EventNotificationHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Webhook endpoint for receiving S3 event notifications.
 * S3 can be configured to send notifications to this endpoint when objects are created/deleted.
 */
@RestController
@RequestMapping("/api/v1/webhooks/s3")
@RequiredArgsConstructor
@Slf4j
public class S3WebhookController {

    private final S3EventNotificationHandler eventHandler;

    /**
     * Receive S3 event notifications.
     * This endpoint should be configured as the destination for S3 event notifications.
     * 
     * @param event S3 event payload
     * @return success response
     */
    @PostMapping("/events")
    public ResponseEntity<Map<String, String>> handleS3Event(@RequestBody Map<String, Object> event) {
        log.info("Received S3 event notification");
        
        try {
            eventHandler.handleS3Event(event);
            return ResponseEntity.ok(Map.of("status", "processed"));
        } catch (Exception e) {
            log.error("Error processing S3 event", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to process event"));
        }
    }

    /**
     * SNS subscription confirmation endpoint.
     * AWS SNS will send a subscription confirmation request when first configured.
     * 
     * @param body SNS message body
     * @return success response
     */
    @PostMapping("/sns-confirm")
    public ResponseEntity<String> handleSnsConfirmation(@RequestBody String body) {
        log.info("Received SNS subscription confirmation");
        // In production, you would parse the SubscribeURL and confirm the subscription
        return ResponseEntity.ok("OK");
    }
}

