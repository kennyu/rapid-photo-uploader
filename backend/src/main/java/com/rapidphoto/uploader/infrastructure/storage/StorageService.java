package com.rapidphoto.uploader.infrastructure.storage;

import java.io.InputStream;
import java.net.URL;
import java.time.Duration;

/**
 * Interface for cloud storage operations.
 * Abstracts storage implementation (S3, Azure Blob, etc.).
 */
public interface StorageService {
    
    /**
     * Generate a pre-signed URL for uploading a file.
     * @param key the object key (file path) in storage
     * @param contentType the content type of the file
     * @param expiration URL expiration duration
     * @return pre-signed URL for upload
     */
    URL generatePresignedUploadUrl(String key, String contentType, Duration expiration);
    
    /**
     * Generate a pre-signed URL for downloading a file.
     * @param key the object key (file path) in storage
     * @param expiration URL expiration duration
     * @return pre-signed URL for download
     */
    URL generatePresignedDownloadUrl(String key, Duration expiration);
    
    /**
     * Upload a file directly to storage.
     * @param key the object key (file path) in storage
     * @param inputStream the file content
     * @param contentType the content type
     * @param contentLength the content length in bytes
     */
    void uploadFile(String key, InputStream inputStream, String contentType, long contentLength);
    
    /**
     * Download a file from storage.
     * @param key the object key (file path) in storage
     * @return input stream of file content
     */
    InputStream downloadFile(String key);
    
    /**
     * Delete a file from storage.
     * @param key the object key (file path) in storage
     */
    void deleteFile(String key);
    
    /**
     * Check if a file exists in storage.
     * @param key the object key (file path) in storage
     * @return true if file exists
     */
    boolean fileExists(String key);
    
    /**
     * Initiate a multipart upload for large files.
     * @param key the object key (file path) in storage
     * @param contentType the content type
     * @return upload ID for tracking the multipart upload
     */
    String initiateMultipartUpload(String key, String contentType);
    
    /**
     * Generate pre-signed URL for uploading a specific part in multipart upload.
     * @param key the object key
     * @param uploadId the multipart upload ID
     * @param partNumber the part number (1-based)
     * @param expiration URL expiration duration
     * @return pre-signed URL for uploading the part
     */
    URL generatePresignedMultipartUploadUrl(String key, String uploadId, int partNumber, Duration expiration);
    
    /**
     * Complete a multipart upload.
     * @param key the object key
     * @param uploadId the multipart upload ID
     * @param eTags list of ETags from uploaded parts
     */
    void completeMultipartUpload(String key, String uploadId, java.util.List<String> eTags);
    
    /**
     * Abort a multipart upload.
     * @param key the object key
     * @param uploadId the multipart upload ID
     */
    void abortMultipartUpload(String key, String uploadId);
}

