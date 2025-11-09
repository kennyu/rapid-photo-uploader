package com.rapidphoto.uploader.api.slices.queryphotos;

import com.rapidphoto.uploader.application.Query;
import com.rapidphoto.uploader.domain.Photo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.UUID;

/**
 * Query to retrieve photos with optional filtering and pagination.
 * Part of CQRS query side.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetPhotosQuery implements Query<Page<Photo>> {
    private UUID userId;
    private Photo.PhotoStatus status;
    private String tag;
    private Integer page;
    private Integer size;
    private String sortBy;
    private String sortDirection;
}

