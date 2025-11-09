package com.rapidphoto.uploader.api.controller;

import com.rapidphoto.uploader.api.dto.PhotoDto;
import com.rapidphoto.uploader.api.mapper.PhotoMapper;
import com.rapidphoto.uploader.domain.Photo;
import com.rapidphoto.uploader.domain.User;
import com.rapidphoto.uploader.infrastructure.repository.PhotoRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.UUID;

/**
 * REST controller for managing photo tags.
 * Allows users to add, remove, and update tags for their photos.
 */
@RestController
@RequestMapping("/api/v1/photos")
@RequiredArgsConstructor
public class PhotoTagController {

    private final PhotoRepository photoRepository;
    private final PhotoMapper photoMapper;

    @Data
    public static class UpdateTagsRequest {
        private Set<String> tags;
    }

    /**
     * Update tags for a photo.
     * Replaces all existing tags with the new set.
     * 
     * @param photoId the photo ID
     * @param request the new tags
     * @param user the authenticated user
     * @return updated photo DTO
     */
    @PatchMapping("/{photoId}/tags")
    public ResponseEntity<PhotoDto> updateTags(
            @PathVariable UUID photoId,
            @RequestBody UpdateTagsRequest request,
            @AuthenticationPrincipal User user) {
        
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new RuntimeException("Photo not found: " + photoId));
        
        // Verify ownership
        if (!photo.getUserId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        // Update tags
        photo.getTags().clear();
        if (request.getTags() != null) {
            photo.getTags().addAll(request.getTags());
        }
        
        Photo savedPhoto = photoRepository.save(photo);
        
        return ResponseEntity.ok(photoMapper.toDto(savedPhoto));
    }

    /**
     * Add a single tag to a photo.
     * 
     * @param photoId the photo ID
     * @param tag the tag to add
     * @param user the authenticated user
     * @return updated photo DTO
     */
    @PostMapping("/{photoId}/tags/{tag}")
    public ResponseEntity<PhotoDto> addTag(
            @PathVariable UUID photoId,
            @PathVariable String tag,
            @AuthenticationPrincipal User user) {
        
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new RuntimeException("Photo not found: " + photoId));
        
        // Verify ownership
        if (!photo.getUserId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        // Add tag
        photo.getTags().add(tag);
        Photo savedPhoto = photoRepository.save(photo);
        
        return ResponseEntity.ok(photoMapper.toDto(savedPhoto));
    }

    /**
     * Remove a single tag from a photo.
     * 
     * @param photoId the photo ID
     * @param tag the tag to remove
     * @param user the authenticated user
     * @return updated photo DTO
     */
    @DeleteMapping("/{photoId}/tags/{tag}")
    public ResponseEntity<PhotoDto> removeTag(
            @PathVariable UUID photoId,
            @PathVariable String tag,
            @AuthenticationPrincipal User user) {
        
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new RuntimeException("Photo not found: " + photoId));
        
        // Verify ownership
        if (!photo.getUserId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        // Remove tag
        photo.getTags().remove(tag);
        Photo savedPhoto = photoRepository.save(photo);
        
        return ResponseEntity.ok(photoMapper.toDto(savedPhoto));
    }
}

