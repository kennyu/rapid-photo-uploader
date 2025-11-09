package com.rapidphoto.uploader.api.slices.uploadphoto;

import com.rapidphoto.uploader.application.Command;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Command to initiate a photo upload.
 * Part of the UploadPhoto vertical slice.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InitiateUploadCommand implements Command<InitiateUploadResult> {
    private UUID userId;
    private String filename;
    private Long fileSize;
    private String contentType;
}

