package com.rapidphoto.uploader.api.slices.queryphotos;

import com.rapidphoto.uploader.api.dto.PhotoDto;
import com.rapidphoto.uploader.api.mapper.PhotoMapper;
import com.rapidphoto.uploader.domain.Photo;
import com.rapidphoto.uploader.infrastructure.repository.PhotoRepository;
import com.rapidphoto.uploader.infrastructure.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URL;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for photo queries (CQRS Query Side).
 * Provides read-only endpoints for retrieving photo metadata.
 */
@RestController
@RequestMapping("/api/v1/photos")
@RequiredArgsConstructor
public class PhotoQueryController {

    private final GetPhotosQueryHandler queryHandler;
    private final PhotoRepository photoRepository;
    private final PhotoMapper photoMapper;
    private final StorageService storageService;
    
    private static final Duration URL_EXPIRATION = Duration.ofHours(1);

    /**
     * Get all photos with optional filtering and pagination.
     * 
     * @param userId filter by user ID
     * @param status filter by photo status
     * @param tag filter by tag
     * @param page page number (0-based)
     * @param size page size
     * @param sortBy field to sort by
     * @param sortDirection sort direction (ASC/DESC)
     * @return paginated photo results
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getPhotos(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) Photo.PhotoStatus status,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        GetPhotosQuery query = GetPhotosQuery.builder()
                .userId(userId)
                .status(status)
                .tag(tag)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        Page<Photo> photoPage = queryHandler.handle(query);
        Page<PhotoDto> dtoPage = photoPage.map(photo -> enrichWithUrls(photoMapper.toDto(photo), photo));

        return ResponseEntity.ok(Map.of(
                "content", dtoPage.getContent(),
                "page", dtoPage.getNumber(),
                "size", dtoPage.getSize(),
                "totalElements", dtoPage.getTotalElements(),
                "totalPages", dtoPage.getTotalPages(),
                "last", dtoPage.isLast()
        ));
    }

    /**
     * Get a specific photo by ID.
     * 
     * @param id photo ID
     * @return photo details
     */
    @GetMapping("/{id}")
    public ResponseEntity<PhotoDto> getPhoto(@PathVariable UUID id) {
        Photo photo = photoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Photo not found: " + id));
        
        PhotoDto dto = photoMapper.toDto(photo);
        enrichWithUrls(dto, photo);
        
        return ResponseEntity.ok(dto);
    }
    
    /**
     * Enrich PhotoDto with pre-signed URLs for thumbnail and full photo.
     * 
     * @param dto the PhotoDto to enrich
     * @param photo the source Photo entity
     * @return enriched PhotoDto
     */
    private PhotoDto enrichWithUrls(PhotoDto dto, Photo photo) {
        try {
            // Generate thumbnail URL
            String thumbnailKey = generateThumbnailKey(photo.getStorageKey());
            URL thumbnailUrl = storageService.generatePresignedDownloadUrl(thumbnailKey, URL_EXPIRATION);
            dto.setThumbnailUrl(thumbnailUrl.toString());
            
            // Generate full photo download URL
            URL downloadUrl = storageService.generatePresignedDownloadUrl(photo.getStorageKey(), URL_EXPIRATION);
            dto.setDownloadUrl(downloadUrl.toString());
        } catch (Exception e) {
            // If URL generation fails, leave URLs as null
            // This prevents errors if thumbnail doesn't exist yet
        }
        
        return dto;
    }
    
    /**
     * Generate thumbnail storage key from original key.
     * Must match the logic in ImageProcessingService.
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
     * Get photo count for a user.
     * 
     * @param userId user ID
     * @return count of photos
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getPhotoCount(@RequestParam UUID userId) {
        long count = photoRepository.countByUserId(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }
}

