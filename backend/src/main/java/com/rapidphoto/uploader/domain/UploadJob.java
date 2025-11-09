package com.rapidphoto.uploader.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain entity representing an upload job tracking status of photo uploads.
 */
@Entity
@Table(name = "upload_jobs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID photoId;

    @Column(nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UploadStatus status;

    @Column
    private String errorMessage;

    @Column(nullable = false)
    private Integer attemptCount;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (attemptCount == null) {
            attemptCount = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public enum UploadStatus {
        PENDING,
        UPLOADING,
        COMPLETE,
        FAILED
    }
}

