package com.rapidphoto.uploader.infrastructure.repository;

import com.rapidphoto.uploader.domain.Photo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for Photo entity.
 * Provides CRUD operations and custom queries.
 */
@Repository
public interface PhotoRepository extends JpaRepository<Photo, UUID> {
    
    /**
     * Find all photos for a specific user.
     * @param userId the user's ID
     * @param pageable pagination information
     * @return page of photos
     */
    Page<Photo> findByUserId(UUID userId, Pageable pageable);
    
    /**
     * Find photos by status.
     * @param status the photo status
     * @param pageable pagination information
     * @return page of photos
     */
    Page<Photo> findByStatus(Photo.PhotoStatus status, Pageable pageable);
    
    /**
     * Find photos by user and status.
     * @param userId the user's ID
     * @param status the photo status
     * @param pageable pagination information
     * @return page of photos
     */
    Page<Photo> findByUserIdAndStatus(UUID userId, Photo.PhotoStatus status, Pageable pageable);
    
    /**
     * Find photos containing a specific tag.
     * @param tag the tag to search for
     * @param pageable pagination information
     * @return page of photos
     */
    @Query("SELECT p FROM Photo p JOIN p.tags t WHERE t = :tag")
    Page<Photo> findByTag(@Param("tag") String tag, Pageable pageable);
    
    /**
     * Count photos by user.
     * @param userId the user's ID
     * @return count of photos
     */
    long countByUserId(UUID userId);
}

