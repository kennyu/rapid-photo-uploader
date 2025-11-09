package com.rapidphoto.uploader.api.slices.queryphotos;

import com.rapidphoto.uploader.application.QueryHandler;
import com.rapidphoto.uploader.domain.Photo;
import com.rapidphoto.uploader.infrastructure.repository.PhotoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handler for GetPhotosQuery.
 * Implements CQRS query pattern for retrieving photo data.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetPhotosQueryHandler implements QueryHandler<GetPhotosQuery, Page<Photo>> {

    private final PhotoRepository photoRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<Photo> handle(GetPhotosQuery query) {
        log.debug("Handling GetPhotosQuery: userId={}, status={}, page={}", 
                query.getUserId(), query.getStatus(), query.getPage());

        Pageable pageable = createPageable(query);

        // Apply filters based on query parameters
        if (query.getUserId() != null && query.getStatus() != null) {
            return photoRepository.findByUserIdAndStatus(query.getUserId(), query.getStatus(), pageable);
        } else if (query.getUserId() != null) {
            return photoRepository.findByUserId(query.getUserId(), pageable);
        } else if (query.getStatus() != null) {
            return photoRepository.findByStatus(query.getStatus(), pageable);
        } else if (query.getTag() != null) {
            return photoRepository.findByTag(query.getTag(), pageable);
        } else {
            return photoRepository.findAll(pageable);
        }
    }

    /**
     * Create pageable object from query parameters.
     */
    private Pageable createPageable(GetPhotosQuery query) {
        int page = query.getPage() != null ? query.getPage() : 0;
        int size = query.getSize() != null ? query.getSize() : 20;
        
        String sortBy = query.getSortBy() != null ? query.getSortBy() : "createdAt";
        String direction = query.getSortDirection() != null ? query.getSortDirection() : "DESC";
        
        Sort.Direction sortDirection = "ASC".equalsIgnoreCase(direction) 
                ? Sort.Direction.ASC 
                : Sort.Direction.DESC;
        
        return PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
    }
}

