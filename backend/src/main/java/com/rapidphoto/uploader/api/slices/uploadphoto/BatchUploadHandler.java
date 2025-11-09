package com.rapidphoto.uploader.api.slices.uploadphoto;

import com.rapidphoto.uploader.domain.Photo;
import com.rapidphoto.uploader.domain.UploadJob;
import com.rapidphoto.uploader.infrastructure.repository.PhotoRepository;
import com.rapidphoto.uploader.infrastructure.repository.UploadJobRepository;
import com.rapidphoto.uploader.infrastructure.storage.StorageKeyGenerator;
import com.rapidphoto.uploader.infrastructure.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handler for batch photo upload initiation.
 * Processes multiple file uploads concurrently and generates pre-signed URLs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BatchUploadHandler {

    private final StorageService storageService;
    private final StorageKeyGenerator keyGenerator;
    private final PhotoRepository photoRepository;
    private final UploadJobRepository uploadJobRepository;
    
    private static final Duration URL_EXPIRATION = Duration.ofHours(1);
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(20);

    /**
     * Process batch upload request and generate pre-signed URLs for all files.
     * Handles up to 100 concurrent uploads asynchronously.
     * 
     * @param request batch upload request
     * @return batch upload response with pre-signed URLs
     */
    public BatchUploadResponse handleBatch(BatchUploadRequest request) {
        log.info("Processing batch upload for user: {}, files: {}", 
                request.getUserId(), request.getFiles().size());
        
        List<CompletableFuture<BatchUploadResponse.UploadResult>> futures = new ArrayList<>();
        
        // Process each file asynchronously
        for (BatchUploadRequest.PhotoMetadata metadata : request.getFiles()) {
            CompletableFuture<BatchUploadResponse.UploadResult> future = 
                    CompletableFuture.supplyAsync(() -> 
                            processFile(request.getUserId(), metadata), EXECUTOR);
            futures.add(future);
        }
        
        // Wait for all uploads to complete
        List<BatchUploadResponse.UploadResult> results = futures.stream()
                .map(CompletableFuture::join)
                .toList();
        
        // Calculate statistics
        long successful = results.stream().filter(BatchUploadResponse.UploadResult::getSuccess).count();
        long failed = results.size() - successful;
        
        log.info("Batch upload completed: total={}, successful={}, failed={}", 
                results.size(), successful, failed);
        
        return BatchUploadResponse.builder()
                .totalFiles(results.size())
                .successfullyInitiated((int) successful)
                .failed((int) failed)
                .uploads(results)
                .build();
    }

    /**
     * Process a single file upload.
     * Creates Photo and UploadJob entities and generates pre-signed URL.
     * 
     * @param userId user ID
     * @param metadata file metadata
     * @return upload result
     */
    @Transactional
    protected BatchUploadResponse.UploadResult processFile(
            java.util.UUID userId, 
            BatchUploadRequest.PhotoMetadata metadata) {
        
        try {
            // Generate unique storage key
            String storageKey = keyGenerator.generateKey(userId, metadata.getFilename());
            
            // Create Photo entity
            Photo photo = Photo.builder()
                    .filename(metadata.getFilename())
                    .fileSize(metadata.getFileSize())
                    .storageKey(storageKey)
                    .userId(userId)
                    .status(Photo.PhotoStatus.UPLOADING)
                    .contentType(metadata.getContentType())
                    .build();
            photo = photoRepository.save(photo);
            
            // Create UploadJob entity
            UploadJob uploadJob = UploadJob.builder()
                    .photoId(photo.getId())
                    .userId(userId)
                    .status(UploadJob.UploadStatus.PENDING)
                    .attemptCount(0)
                    .build();
            uploadJob = uploadJobRepository.save(uploadJob);
            
            // Generate pre-signed URL
            URL presignedUrl = storageService.generatePresignedUploadUrl(
                    storageKey, 
                    metadata.getContentType(), 
                    URL_EXPIRATION
            );
            
            log.debug("Successfully initiated upload for file: {}", metadata.getFilename());
            
            return BatchUploadResponse.UploadResult.builder()
                    .uploadJobId(uploadJob.getId())
                    .photoId(photo.getId())
                    .filename(metadata.getFilename())
                    .preSignedUrl(presignedUrl.toString())
                    .expiresInSeconds((int) URL_EXPIRATION.getSeconds())
                    .success(true)
                    .build();
            
        } catch (Exception e) {
            log.error("Failed to process file: {}", metadata.getFilename(), e);
            
            return BatchUploadResponse.UploadResult.builder()
                    .filename(metadata.getFilename())
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }
}

