package com.rapidphoto.uploader.infrastructure.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for AWS S3 storage.
 * Binds to application properties prefixed with 'aws.s3'.
 */
@Component
@ConfigurationProperties(prefix = "aws.s3")
@Data
public class S3StorageProperties {
    
    /**
     * S3 bucket name for storing photos.
     */
    private String bucketName;
    
    /**
     * AWS region for S3 bucket.
     */
    private String region = "us-east-1";
    
    /**
     * Pre-signed URL expiration time in seconds.
     */
    private Integer presignedUrlExpirationSeconds = 3600; // 1 hour default
}

