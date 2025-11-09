package com.rapidphoto.uploader.infrastructure.scheduler;

import com.rapidphoto.uploader.application.service.UploadStatusService;
import com.rapidphoto.uploader.domain.UploadJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Scheduled job to process failed upload jobs and retry them.
 * Runs every 5 minutes to check for failed uploads and initiate retries.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "upload.retry.scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
public class UploadRetryScheduler {

    private final UploadStatusService uploadStatusService;

    /**
     * Process failed upload jobs and retry eligible ones.
     * Runs according to configured cron expression (default: every 5 minutes).
     */
    @Scheduled(cron = "${upload.retry.scheduler.cron:0 */5 * * * *}")
    public void processFailedUploads() {
        log.info("Starting scheduled retry processing for failed uploads");
        
        try {
            List<UploadJob> failedJobs = uploadStatusService.getRetryableFailedJobs();
            
            if (failedJobs.isEmpty()) {
                log.debug("No failed uploads found for retry");
                return;
            }
            
            log.info("Found {} failed upload jobs eligible for retry", failedJobs.size());
            
            int successCount = 0;
            int failCount = 0;
            
            for (UploadJob job : failedJobs) {
                try {
                    uploadStatusService.retryUpload(job.getId());
                    successCount++;
                } catch (Exception e) {
                    log.error("Failed to retry upload job: {}", job.getId(), e);
                    failCount++;
                }
            }
            
            log.info("Retry processing completed: successful={}, failed={}", successCount, failCount);
            
        } catch (Exception e) {
            log.error("Error during scheduled retry processing", e);
        }
    }
}

