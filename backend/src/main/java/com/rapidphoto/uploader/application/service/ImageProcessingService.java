package com.rapidphoto.uploader.application.service;

import com.rapidphoto.uploader.domain.Photo;
import com.rapidphoto.uploader.infrastructure.repository.PhotoRepository;
import com.rapidphoto.uploader.infrastructure.storage.StorageService;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

/**
 * Service for processing images after upload.
 * Handles compression, thumbnail generation, and optional AI tagging.
 */
@Service
@Slf4j
public class ImageProcessingService {

    private final StorageService storageService;
    private final PhotoRepository photoRepository;
    
    @Autowired(required = false)
    private ImageTaggingService taggingService;
    
    public ImageProcessingService(StorageService storageService, PhotoRepository photoRepository) {
        this.storageService = storageService;
        this.photoRepository = photoRepository;
    }

    private static final double COMPRESSION_QUALITY = 0.85; // 85% quality
    private static final int THUMBNAIL_SIZE = 300; // 300px thumbnail

    /**
     * Process image asynchronously after upload.
     * Compresses image, generates thumbnail, and applies AI tagging if enabled.
     * 
     * @param photoId the photo ID to process
     */
    @Async
    @Transactional
    public void processImageAsync(UUID photoId) {
        try {
            log.info("Starting async image processing for photo: {}", photoId);
            
            Photo photo = photoRepository.findById(photoId)
                    .orElseThrow(() -> new RuntimeException("Photo not found: " + photoId));
            
            log.info("Waiting 2 seconds for S3 eventual consistency...");
            Thread.sleep(2000); // Wait for S3 eventual consistency
            
            // Check if file exists before downloading
            boolean fileExists = storageService.fileExists(photo.getStorageKey());
            log.info("File exists check for key {}: {}", photo.getStorageKey(), fileExists);
            
            if (!fileExists) {
                throw new RuntimeException("File not found in S3 after waiting: " + photo.getStorageKey());
            }
            
            // Download original image from S3
            log.info("Downloading file from S3 with key: {}", photo.getStorageKey());
            InputStream originalImage = storageService.downloadFile(photo.getStorageKey());
            
            // Compress image
            ByteArrayOutputStream compressedOutput = new ByteArrayOutputStream();
            compressImage(originalImage, compressedOutput, COMPRESSION_QUALITY);
            byte[] compressedBytes = compressedOutput.toByteArray();
            
            // Upload compressed version (overwrite original)
            storageService.uploadFile(
                    photo.getStorageKey(),
                    new ByteArrayInputStream(compressedBytes),
                    photo.getContentType(),
                    compressedBytes.length
            );
            
            // Generate and upload thumbnail
            String thumbnailKey = generateThumbnailKey(photo.getStorageKey());
            ByteArrayOutputStream thumbnailOutput = new ByteArrayOutputStream();
            generateThumbnail(new ByteArrayInputStream(compressedBytes), thumbnailOutput, THUMBNAIL_SIZE);
            byte[] thumbnailBytes = thumbnailOutput.toByteArray();
            
            storageService.uploadFile(
                    thumbnailKey,
                    new ByteArrayInputStream(thumbnailBytes),
                    photo.getContentType(),
                    thumbnailBytes.length
            );
            
            // Apply AI tagging if enabled
            Set<String> tags = null;
            if (taggingService != null) {
                tags = taggingService.generateTags(new ByteArrayInputStream(compressedBytes));
                if (tags != null && !tags.isEmpty()) {
                    photo.getTags().addAll(tags);
                }
            }
            
            // Update photo metadata
            photo.setFileSize((long) compressedBytes.length);
            photo.setStatus(Photo.PhotoStatus.COMPLETE);
            photoRepository.save(photo);
            
            log.info("Completed image processing for photo: {}, compressed size: {} bytes, tags: {}", 
                    photoId, compressedBytes.length, tags != null ? tags.size() : 0);
            
        } catch (Exception e) {
            log.error("Failed to process image: {}", photoId, e);
            markProcessingFailed(photoId, e.getMessage());
        }
    }

    /**
     * Compress image with specified quality.
     * 
     * @param input input stream of original image
     * @param output output stream for compressed image
     * @param quality compression quality (0.0 to 1.0)
     */
    private void compressImage(InputStream input, ByteArrayOutputStream output, double quality) throws Exception {
        Thumbnails.of(input)
                .scale(1.0) // Keep original dimensions
                .outputQuality(quality)
                .toOutputStream(output);
    }

    /**
     * Generate thumbnail of specified size.
     * 
     * @param input input stream of image
     * @param output output stream for thumbnail
     * @param size thumbnail size (width/height)
     */
    private void generateThumbnail(InputStream input, ByteArrayOutputStream output, int size) throws Exception {
        Thumbnails.of(input)
                .size(size, size)
                .keepAspectRatio(true)
                .toOutputStream(output);
    }

    /**
     * Generate thumbnail storage key from original key.
     * 
     * @param originalKey original storage key
     * @return thumbnail storage key
     */
    private String generateThumbnailKey(String originalKey) {
        int lastDotIndex = originalKey.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return originalKey.substring(0, lastDotIndex) + "_thumb" + originalKey.substring(lastDotIndex);
        }
        return originalKey + "_thumb";
    }

    /**
     * Mark photo processing as failed.
     * 
     * @param photoId photo ID
     * @param errorMessage error message
     */
    @Transactional
    protected void markProcessingFailed(UUID photoId, String errorMessage) {
        photoRepository.findById(photoId).ifPresent(photo -> {
            photo.setStatus(Photo.PhotoStatus.FAILED);
            photoRepository.save(photo);
        });
    }
}

