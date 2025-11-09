package com.rapidphoto.uploader.api.mapper;

import com.rapidphoto.uploader.api.dto.PhotoDto;
import com.rapidphoto.uploader.domain.Photo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

/**
 * MapStruct mapper for converting between Photo entities and DTOs.
 * Demonstrates MapStruct integration for DTO mapping.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PhotoMapper {

    // Convert Photo entity to DTO
    // thumbnailUrl and downloadUrl are set manually in the controller with pre-signed URLs
    @Mapping(target = "thumbnailUrl", ignore = true)
    @Mapping(target = "downloadUrl", ignore = true)
    PhotoDto toDto(Photo photo);

    // Convert list of Photo entities to list of DTOs
    List<PhotoDto> toDtoList(List<Photo> photos);
}

