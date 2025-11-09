package com.rapidphoto.uploader.api.mapper;

import com.rapidphoto.uploader.api.dto.UploadJobDto;
import com.rapidphoto.uploader.domain.UploadJob;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

/**
 * MapStruct mapper for converting between UploadJob entities and DTOs.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UploadJobMapper {

    UploadJobDto toDto(UploadJob uploadJob);

    UploadJob toEntity(UploadJobDto dto);

    List<UploadJobDto> toDtoList(List<UploadJob> uploadJobs);
}

