package com.rapidphoto.uploader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rapidphoto.uploader.api.dto.AuthRequestDto;
import com.rapidphoto.uploader.api.dto.RegisterRequestDto;
import com.rapidphoto.uploader.domain.model.Photo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.jdbc.Sql;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the complete photo upload flow.
 * Tests: Registration -> Login -> Upload Initiation -> Upload Completion -> Status Check
 */
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class PhotoUploadIntegrationTest extends IntegrationTestBase {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private String authToken;
    private UUID userId;

    @BeforeEach
    void setUp() {
        // Register and login for each test
        registerAndLogin();
    }

    @Test
    void shouldCompleteFullUploadFlow() {
        // Step 1: Initiate upload
        Map<String, Object> initiateRequest = Map.of(
                "filename", "test-photo.jpg",
                "fileSize", 1024000,
                "contentType", "image/jpeg"
        );

        HttpEntity<Map<String, Object>> initiateEntity = new HttpEntity<>(
                initiateRequest,
                createAuthHeaders()
        );

        ResponseEntity<Map> initiateResponse = restTemplate.exchange(
                baseUrl() + "/api/v1/photos/upload/initiate",
                HttpMethod.POST,
                initiateEntity,
                Map.class
        );

        assertThat(initiateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(initiateResponse.getBody()).isNotNull();
        assertThat(initiateResponse.getBody()).containsKeys("photoId", "uploadJobId", "preSignedUrl");

        String photoId = (String) initiateResponse.getBody().get("photoId");
        String uploadJobId = (String) initiateResponse.getBody().get("uploadJobId");
        String preSignedUrl = (String) initiateResponse.getBody().get("preSignedUrl");

        assertThat(photoId).isNotNull();
        assertThat(uploadJobId).isNotNull();
        assertThat(preSignedUrl).isNotNull();
        assertThat(preSignedUrl).startsWith("https://");

        // Step 2: Simulate S3 upload completion by marking upload complete
        ResponseEntity<Void> completeResponse = restTemplate.exchange(
                baseUrl() + "/api/v1/uploads/" + uploadJobId + "/complete",
                HttpMethod.POST,
                new HttpEntity<>(createAuthHeaders()),
                Void.class
        );

        assertThat(completeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Step 3: Verify photo status
        // Give a moment for async processing
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        ResponseEntity<Map> photosResponse = restTemplate.exchange(
                baseUrl() + "/api/v1/photos",
                HttpMethod.GET,
                new HttpEntity<>(createAuthHeaders()),
                Map.class
        );

        assertThat(photosResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(photosResponse.getBody()).isNotNull();
        assertThat(photosResponse.getBody()).containsKey("content");
        
        // Verify the photo is in the list
        var content = (java.util.List<?>) photosResponse.getBody().get("content");
        assertThat(content).isNotEmpty();
        
        // Photo should be COMPLETE or PROCESSING (since we disabled actual image processing)
        Map<String, Object> photo = (Map<String, Object>) content.get(0);
        assertThat(photo.get("filename")).isEqualTo("test-photo.jpg");
        String status = (String) photo.get("status");
        assertThat(status).isIn("COMPLETE", "PROCESSING", "UPLOADING");
    }

    @Test
    void shouldFilterPhotosByTag() {
        // Upload and tag a photo first
        String photoId = uploadPhoto("tagged-photo.jpg");

        // Add a tag
        ResponseEntity<Void> tagResponse = restTemplate.exchange(
                baseUrl() + "/api/v1/photos/" + photoId + "/tags/vacation",
                HttpMethod.POST,
                new HttpEntity<>(createAuthHeaders()),
                Void.class
        );

        assertThat(tagResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Filter by tag
        ResponseEntity<Map> filterResponse = restTemplate.exchange(
                baseUrl() + "/api/v1/photos?tag=vacation",
                HttpMethod.GET,
                new HttpEntity<>(createAuthHeaders()),
                Map.class
        );

        assertThat(filterResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        var content = (java.util.List<?>) filterResponse.getBody().get("content");
        assertThat(content).hasSize(1);

        Map<String, Object> photo = (Map<String, Object>) content.get(0);
        var tags = (java.util.List<?>) photo.get("tags");
        assertThat(tags).contains("vacation");
    }

    @Test
    void shouldHandleBatchUploadInitiation() {
        // Initiate multiple uploads
        int batchSize = 10;
        
        for (int i = 0; i < batchSize; i++) {
            Map<String, Object> request = Map.of(
                    "filename", "batch-photo-" + i + ".jpg",
                    "fileSize", 1024000 + i,
                    "contentType", "image/jpeg"
            );

            ResponseEntity<Map> response = restTemplate.exchange(
                    baseUrl() + "/api/v1/photos/upload/initiate",
                    HttpMethod.POST,
                    new HttpEntity<>(request, createAuthHeaders()),
                    Map.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).containsKeys("photoId", "uploadJobId", "preSignedUrl");
        }

        // Verify all photos are in the database
        ResponseEntity<Map> photosResponse = restTemplate.exchange(
                baseUrl() + "/api/v1/photos?size=20",
                HttpMethod.GET,
                new HttpEntity<>(createAuthHeaders()),
                Map.class
        );

        var content = (java.util.List<?>) photosResponse.getBody().get("content");
        assertThat(content).hasSizeGreaterThanOrEqualTo(batchSize);
    }

    @Test
    void shouldRequireAuthenticationForUpload() {
        Map<String, Object> request = Map.of(
                "filename", "test.jpg",
                "fileSize", 1024000,
                "contentType", "image/jpeg"
        );

        // No auth header
        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl() + "/api/v1/photos/upload/initiate",
                request,
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldValidateUploadRequest() {
        // Missing required fields
        Map<String, Object> invalidRequest = Map.of(
                "filename", ""  // Empty filename
        );

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/v1/photos/upload/initiate",
                HttpMethod.POST,
                new HttpEntity<>(invalidRequest, createAuthHeaders()),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // Helper methods

    private void registerAndLogin() {
        String testEmail = "test-" + UUID.randomUUID() + "@example.com";
        String testPassword = "TestPassword123!";

        // Register
        RegisterRequestDto registerRequest = new RegisterRequestDto();
        registerRequest.setEmail(testEmail);
        registerRequest.setPassword(testPassword);
        registerRequest.setFullName("Test User");

        ResponseEntity<Map> registerResponse = restTemplate.postForEntity(
                baseUrl() + "/api/v1/auth/register",
                registerRequest,
                Map.class
        );

        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(registerResponse.getBody()).containsKey("token");

        authToken = (String) registerResponse.getBody().get("token");
        userId = UUID.fromString((String) registerResponse.getBody().get("userId"));
    }

    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(authToken);
        return headers;
    }

    private String uploadPhoto(String filename) {
        Map<String, Object> request = Map.of(
                "filename", filename,
                "fileSize", 1024000,
                "contentType", "image/jpeg"
        );

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/v1/photos/upload/initiate",
                HttpMethod.POST,
                new HttpEntity<>(request, createAuthHeaders()),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return (String) response.getBody().get("photoId");
    }
}

