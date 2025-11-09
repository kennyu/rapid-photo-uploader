package com.rapidphoto.uploader.infrastructure.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedUploadPartRequest;

import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.stream.IntStream;

/**
 * AWS S3 implementation of StorageService.
 * Handles file uploads, downloads, and pre-signed URL generation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class S3StorageService implements StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3StorageProperties properties;

    @Override
    public URL generatePresignedUploadUrl(String key, String contentType, Duration expiration) {
        log.info("Generating pre-signed upload URL for key: {}", key);
        
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(properties.getBucketName())
                .key(key)
                .contentType(contentType)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(builder ->
                builder.putObjectRequest(putObjectRequest)
                        .signatureDuration(expiration)
        );

        return presignedRequest.url();
    }

    @Override
    public URL generatePresignedDownloadUrl(String key, Duration expiration) {
        log.info("Generating pre-signed download URL for key: {}", key);
        
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(properties.getBucketName())
                .key(key)
                .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(builder ->
                builder.getObjectRequest(getObjectRequest)
                        .signatureDuration(expiration)
        );

        return presignedRequest.url();
    }

    @Override
    public void uploadFile(String key, InputStream inputStream, String contentType, long contentLength) {
        log.info("Uploading file with key: {}, size: {} bytes", key, contentLength);
        
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(key)
                    .contentType(contentType)
                    .contentLength(contentLength)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, contentLength));
            log.info("Successfully uploaded file: {}", key);
        } catch (S3Exception e) {
            log.error("Failed to upload file: {}", key, e);
            throw new RuntimeException("Failed to upload file to S3", e);
        }
    }

    @Override
    public InputStream downloadFile(String key) {
        log.info("Downloading file with key: {}", key);
        
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(key)
                    .build();

            return s3Client.getObject(getObjectRequest);
        } catch (NoSuchKeyException e) {
            log.error("File not found: {}", key);
            throw new RuntimeException("File not found in S3: " + key, e);
        } catch (S3Exception e) {
            log.error("Failed to download file: {}", key, e);
            throw new RuntimeException("Failed to download file from S3", e);
        }
    }

    @Override
    public void deleteFile(String key) {
        log.info("Deleting file with key: {}", key);
        
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("Successfully deleted file: {}", key);
        } catch (S3Exception e) {
            log.error("Failed to delete file: {}", key, e);
            throw new RuntimeException("Failed to delete file from S3", e);
        }
    }

    @Override
    public boolean fileExists(String key) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(key)
                    .build();

            s3Client.headObject(headObjectRequest);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            log.error("Error checking if file exists: {}", key, e);
            throw new RuntimeException("Failed to check file existence in S3", e);
        }
    }

    @Override
    public String initiateMultipartUpload(String key, String contentType) {
        log.info("Initiating multipart upload for key: {}", key);
        
        try {
            CreateMultipartUploadRequest createRequest = CreateMultipartUploadRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(key)
                    .contentType(contentType)
                    .build();

            CreateMultipartUploadResponse response = s3Client.createMultipartUpload(createRequest);
            String uploadId = response.uploadId();
            log.info("Initiated multipart upload with ID: {} for key: {}", uploadId, key);
            return uploadId;
        } catch (S3Exception e) {
            log.error("Failed to initiate multipart upload: {}", key, e);
            throw new RuntimeException("Failed to initiate multipart upload", e);
        }
    }

    @Override
    public URL generatePresignedMultipartUploadUrl(String key, String uploadId, int partNumber, Duration expiration) {
        log.info("Generating pre-signed URL for multipart upload part: key={}, uploadId={}, partNumber={}",
                key, uploadId, partNumber);
        
        UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                .bucket(properties.getBucketName())
                .key(key)
                .uploadId(uploadId)
                .partNumber(partNumber)
                .build();

        PresignedUploadPartRequest presignedRequest = s3Presigner.presignUploadPart(builder ->
                builder.uploadPartRequest(uploadPartRequest)
                        .signatureDuration(expiration)
        );

        return presignedRequest.url();
    }

    @Override
    public void completeMultipartUpload(String key, String uploadId, List<String> eTags) {
        log.info("Completing multipart upload: key={}, uploadId={}, parts={}", key, uploadId, eTags.size());
        
        try {
            List<CompletedPart> completedParts = IntStream.range(0, eTags.size())
                    .mapToObj(i -> CompletedPart.builder()
                            .partNumber(i + 1)
                            .eTag(eTags.get(i))
                            .build())
                    .toList();

            CompletedMultipartUpload completedUpload = CompletedMultipartUpload.builder()
                    .parts(completedParts)
                    .build();

            CompleteMultipartUploadRequest completeRequest = CompleteMultipartUploadRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(key)
                    .uploadId(uploadId)
                    .multipartUpload(completedUpload)
                    .build();

            s3Client.completeMultipartUpload(completeRequest);
            log.info("Successfully completed multipart upload: {}", key);
        } catch (S3Exception e) {
            log.error("Failed to complete multipart upload: key={}, uploadId={}", key, uploadId, e);
            throw new RuntimeException("Failed to complete multipart upload", e);
        }
    }

    @Override
    public void abortMultipartUpload(String key, String uploadId) {
        log.info("Aborting multipart upload: key={}, uploadId={}", key, uploadId);
        
        try {
            AbortMultipartUploadRequest abortRequest = AbortMultipartUploadRequest.builder()
                    .bucket(properties.getBucketName())
                    .key(key)
                    .uploadId(uploadId)
                    .build();

            s3Client.abortMultipartUpload(abortRequest);
            log.info("Successfully aborted multipart upload: {}", key);
        } catch (S3Exception e) {
            log.error("Failed to abort multipart upload: key={}, uploadId={}", key, uploadId, e);
            throw new RuntimeException("Failed to abort multipart upload", e);
        }
    }
}

