package com.rapidphoto.uploader.api.slices.uploadphoto;

import com.rapidphoto.uploader.application.CommandHandler;
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

/**
 * Handler for InitiateUploadCommand.
 * Demonstrates CQRS command handler pattern and vertical slice architecture.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InitiateUploadHandler implements CommandHandler<InitiateUploadCommand, InitiateUploadResult> {

    private final StorageService storageService;
    private final StorageKeyGenerator keyGenerator;
    private final PhotoRepository photoRepository;
    private final UploadJobRepository uploadJobRepository;
    
    private static final Duration URL_EXPIRATION = Duration.ofHours(1);

    @Override
    @Transactional
    public InitiateUploadResult handle(InitiateUploadCommand command) {
        log.info("Initiating upload for user: {}, file: {}", 
                command.getUserId(), command.getFilename());

        // Generate unique storage key
        String storageKey = keyGenerator.generateKey(command.getUserId(), command.getFilename());
        
        // Create Photo entity
        Photo photo = Photo.builder()
                .filename(command.getFilename())
                .fileSize(command.getFileSize())
                .storageKey(storageKey)
                .userId(command.getUserId())
                .status(Photo.PhotoStatus.UPLOADING)
                .contentType(command.getContentType())
                .build();
        photo = photoRepository.save(photo);
        
        // Create UploadJob entity
        UploadJob uploadJob = UploadJob.builder()
                .photoId(photo.getId())
                .userId(command.getUserId())
                .status(UploadJob.UploadStatus.PENDING)
                .attemptCount(0)
                .build();
        uploadJob = uploadJobRepository.save(uploadJob);
        
        // Generate pre-signed URL for direct client upload
        URL presignedUrl = storageService.generatePresignedUploadUrl(
                storageKey, 
                command.getContentType(), 
                URL_EXPIRATION
        );
        
        log.info("Generated pre-signed URL for photo: {}, uploadJob: {}", 
                photo.getId(), uploadJob.getId());
        
        return InitiateUploadResult.builder()
                .uploadJobId(uploadJob.getId())
                .photoId(photo.getId())
                .preSignedUrl(presignedUrl.toString())
                .expiresInSeconds((int) URL_EXPIRATION.getSeconds())
                .build();
    }
}

