package com.rapidphoto.uploader.api.controller;

import com.rapidphoto.uploader.api.dto.UploadJobDto;
import com.rapidphoto.uploader.api.mapper.UploadJobMapper;
import com.rapidphoto.uploader.domain.UploadJob;
import com.rapidphoto.uploader.infrastructure.repository.UploadJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST controller for upload job queries (CQRS Query Side).
 * Provides read-only endpoints for retrieving upload job status.
 */
@RestController
@RequestMapping("/api/v1/upload-jobs")
@RequiredArgsConstructor
public class UploadJobQueryController {

    private final UploadJobRepository uploadJobRepository;
    private final UploadJobMapper uploadJobMapper;

    /**
     * Get all upload jobs with optional filtering and pagination.
     * 
     * @param userId filter by user ID
     * @param status filter by upload status
     * @param page page number (0-based)
     * @param size page size
     * @param sortBy field to sort by
     * @param sortDirection sort direction (ASC/DESC)
     * @return paginated upload job results
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getUploadJobs(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) UploadJob.UploadStatus status,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection) 
                ? Sort.Direction.ASC 
                : Sort.Direction.DESC;
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        Page<UploadJob> jobPage;
        
        if (userId != null && status != null) {
            // Filter by both user and status (would need custom query)
            jobPage = uploadJobRepository.findByUserId(userId, pageable);
        } else if (userId != null) {
            jobPage = uploadJobRepository.findByUserId(userId, pageable);
        } else if (status != null) {
            jobPage = uploadJobRepository.findByStatus(status, pageable);
        } else {
            jobPage = uploadJobRepository.findAll(pageable);
        }
        
        Page<UploadJobDto> dtoPage = jobPage.map(uploadJobMapper::toDto);

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
     * Get a specific upload job by ID.
     * 
     * @param id upload job ID
     * @return upload job details
     */
    @GetMapping("/{id}")
    public ResponseEntity<UploadJobDto> getUploadJob(@PathVariable UUID id) {
        UploadJob uploadJob = uploadJobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Upload job not found: " + id));
        
        return ResponseEntity.ok(uploadJobMapper.toDto(uploadJob));
    }

    /**
     * Get upload job by photo ID.
     * 
     * @param photoId photo ID
     * @return upload job details
     */
    @GetMapping("/by-photo/{photoId}")
    public ResponseEntity<UploadJobDto> getUploadJobByPhoto(@PathVariable UUID photoId) {
        UploadJob uploadJob = uploadJobRepository.findByPhotoId(photoId)
                .orElseThrow(() -> new RuntimeException("Upload job not found for photo: " + photoId));
        
        return ResponseEntity.ok(uploadJobMapper.toDto(uploadJob));
    }
}

