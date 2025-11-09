package com.rapidphoto.uploader.infrastructure.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * Configuration for AWS S3 clients.
 * Creates S3Client and S3Presigner beans using credentials from environment or AWS credentials chain.
 */
@Configuration
@RequiredArgsConstructor
public class S3Config {

    private final S3StorageProperties properties;

    /**
     * Creates an S3Client for direct S3 operations.
     * Uses DefaultCredentialsProvider which checks:
     * 1. Environment variables (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY)
     * 2. System properties
     * 3. AWS credentials file (~/.aws/credentials)
     * 4. IAM role (if running on EC2/ECS)
     */
    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    /**
     * Creates an S3Presigner for generating pre-signed URLs.
     */
    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}

