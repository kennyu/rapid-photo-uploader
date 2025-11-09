package com.rapidphoto.uploader.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain entity representing a photo in the system.
 * Part of the core domain model.
 */
@Entity
@Table(name = "photos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Photo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String filename;

    @Column(nullable = false)
    private Long fileSize;

    @Column(nullable = false)
    private String storageKey;

    @Column(nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PhotoStatus status;

    @Column
    private String contentType;

    @ElementCollection
    @CollectionTable(name = "photo_tags", joinColumns = @JoinColumn(name = "photo_id"))
    @Column(name = "tag")
    @Builder.Default
    private java.util.Set<String> tags = new java.util.HashSet<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public enum PhotoStatus {
        UPLOADING,
        PROCESSING,
        COMPLETE,
        FAILED
    }
}

