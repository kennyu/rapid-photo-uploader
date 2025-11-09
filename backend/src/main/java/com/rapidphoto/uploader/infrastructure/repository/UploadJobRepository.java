package com.rapidphoto.uploader.infrastructure.repository;

import com.rapidphoto.uploader.domain.UploadJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for UploadJob entity.
 * Provides CRUD operations and custom queries.
 */
@Repository
public interface UploadJobRepository extends JpaRepository<UploadJob, UUID> {
    
    /**
     * Find upload job by photo ID.
     * @param photoId the photo's ID
     * @return optional upload job
     */
    Optional<UploadJob> findByPhotoId(UUID photoId);
    
    /**
     * Find all upload jobs for a specific user.
     * @param userId the user's ID
     * @param pageable pagination information
     * @return page of upload jobs
     */
    Page<UploadJob> findByUserId(UUID userId, Pageable pageable);
    
    /**
     * Find upload jobs by status.
     * @param status the upload status
     * @param pageable pagination information
     * @return page of upload jobs
     */
    Page<UploadJob> findByStatus(UploadJob.UploadStatus status, Pageable pageable);
    
    /**
     * Find failed upload jobs with attempt count less than max.
     * Used for retry logic.
     * @param status the status (FAILED)
     * @param maxAttempts maximum attempts
     * @return list of failed jobs that can be retried
     */
    List<UploadJob> findByStatusAndAttemptCountLessThan(
            UploadJob.UploadStatus status, 
            Integer maxAttempts
    );
}

