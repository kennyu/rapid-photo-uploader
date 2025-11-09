package com.rapidphoto.uploader.application.service;

import com.rapidphoto.uploader.domain.Photo;
import com.rapidphoto.uploader.domain.UploadJob;
import com.rapidphoto.uploader.infrastructure.repository.PhotoRepository;
import com.rapidphoto.uploader.infrastructure.repository.UploadJobRepository;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing upload job status updates and retries.
 * Handles status transitions and retry logic with exponential backoff.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UploadStatusService {

    private final UploadJobRepository uploadJobRepository;
    private final PhotoRepository photoRepository;
    private final ImageProcessingService imageProcessingService;
    
    @Value("${image.processing.enabled:true}")
    private boolean imageProcessingEnabled;
    
    private static final int MAX_RETRY_ATTEMPTS = 3;

    /**
     * Update upload job status.
     * @param uploadJobId the upload job ID
     * @param status the new status
     * @param errorMessage optional error message for failed uploads
     */
    @Transactional
    public void updateStatus(UUID uploadJobId, UploadJob.UploadStatus status, String errorMessage) {
        log.info("Updating upload job status: jobId={}, status={}", uploadJobId, status);
        
        UploadJob uploadJob = uploadJobRepository.findById(uploadJobId)
                .orElseThrow(() -> new RuntimeException("Upload job not found: " + uploadJobId));
        
        uploadJob.setStatus(status);
        
        if (errorMessage != null) {
            uploadJob.setErrorMessage(errorMessage);
        }
        
        // Update associated photo status
        Photo photo = photoRepository.findById(uploadJob.getPhotoId())
                .orElseThrow(() -> new RuntimeException("Photo not found: " + uploadJob.getPhotoId()));
        
        switch (status) {
            case UPLOADING -> photo.setStatus(Photo.PhotoStatus.UPLOADING);
            case COMPLETE -> photo.setStatus(Photo.PhotoStatus.COMPLETE);
            case FAILED -> photo.setStatus(Photo.PhotoStatus.FAILED);
        }
        
        uploadJobRepository.save(uploadJob);
        photoRepository.save(photo);
        
        log.info("Updated status for upload job: {} and photo: {}", uploadJobId, photo.getId());
    }

    /**
     * Mark upload as complete and trigger image processing.
     * @param uploadJobId the upload job ID
     */
    @Transactional
    public void markComplete(UUID uploadJobId) {
        updateStatus(uploadJobId, UploadJob.UploadStatus.COMPLETE, null);
        
        // Trigger async image processing if enabled
        if (imageProcessingEnabled) {
            UploadJob uploadJob = uploadJobRepository.findById(uploadJobId)
                    .orElseThrow(() -> new RuntimeException("Upload job not found: " + uploadJobId));
            
            log.info("Triggering image processing for photo: {}", uploadJob.getPhotoId());
            imageProcessingService.processImageAsync(uploadJob.getPhotoId());
        }
    }

    /**
     * Mark upload as failed and increment retry counter.
     * @param uploadJobId the upload job ID
     * @param errorMessage error message
     */
    @Transactional
    public void markFailed(UUID uploadJobId, String errorMessage) {
        UploadJob uploadJob = uploadJobRepository.findById(uploadJobId)
                .orElseThrow(() -> new RuntimeException("Upload job not found: " + uploadJobId));
        
        uploadJob.setAttemptCount(uploadJob.getAttemptCount() + 1);
        uploadJob.setStatus(UploadJob.UploadStatus.FAILED);
        uploadJob.setErrorMessage(errorMessage);
        
        uploadJobRepository.save(uploadJob);
        
        log.warn("Upload job failed: jobId={}, attempts={}, error={}", 
                uploadJobId, uploadJob.getAttemptCount(), errorMessage);
    }

    /**
     * Get failed upload jobs that are eligible for retry.
     * @return list of failed jobs with attempts < max
     */
    @Transactional(readOnly = true)
    public List<UploadJob> getRetryableFailedJobs() {
        return uploadJobRepository.findByStatusAndAttemptCountLessThan(
                UploadJob.UploadStatus.FAILED, 
                MAX_RETRY_ATTEMPTS
        );
    }

    /**
     * Retry a failed upload job with exponential backoff.
     * Uses Resilience4j retry mechanism.
     * @param uploadJobId the upload job ID
     */
    @Retry(name = "uploadRetry", fallbackMethod = "retryUploadFallback")
    @Transactional
    public void retryUpload(UUID uploadJobId) {
        log.info("Retrying upload job: {}", uploadJobId);
        
        UploadJob uploadJob = uploadJobRepository.findById(uploadJobId)
                .orElseThrow(() -> new RuntimeException("Upload job not found: " + uploadJobId));
        
        // Reset status to UPLOADING for retry
        uploadJob.setStatus(UploadJob.UploadStatus.UPLOADING);
        uploadJob.setAttemptCount(uploadJob.getAttemptCount() + 1);
        uploadJobRepository.save(uploadJob);
        
        log.info("Upload job retry initiated: jobId={}, attempt={}", 
                uploadJobId, uploadJob.getAttemptCount());
    }

    /**
     * Fallback method when all retry attempts are exhausted.
     * @param uploadJobId the upload job ID
     * @param ex the exception that caused retry failure
     */
    public void retryUploadFallback(UUID uploadJobId, Exception ex) {
        log.error("All retry attempts exhausted for upload job: {}", uploadJobId, ex);
        markFailed(uploadJobId, "Max retry attempts exceeded: " + ex.getMessage());
    }

    /**
     * Check if upload job can be retried.
     * @param uploadJobId the upload job ID
     * @return true if retry is possible
     */
    public boolean canRetry(UUID uploadJobId) {
        return uploadJobRepository.findById(uploadJobId)
                .map(job -> job.getAttemptCount() < MAX_RETRY_ATTEMPTS)
                .orElse(false);
    }
}

