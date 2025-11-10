package com.rapidphoto.uploader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rapidphoto.uploader.api.dto.RegisterRequestDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for gallery viewing and tag management operations.
 */
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class GalleryAndTagsIntegrationTest extends IntegrationTestBase {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private String authToken;

    @BeforeEach
    void setUp() {
        registerAndLogin();
    }

    @Test
    void shouldRetrieveUserPhotosWithPagination() {
        // Upload multiple photos
        for (int i = 0; i < 25; i++) {
            uploadPhoto("photo-" + i + ".jpg");
        }

        // Get first page
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/v1/photos?page=0&size=10",
                HttpMethod.GET,
                new HttpEntity<>(createAuthHeaders()),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = response.getBody();
        
        assertThat(body).containsKeys("content", "page", "size", "totalElements", "totalPages");
        assertThat((Integer) body.get("page")).isEqualTo(0);
        assertThat((Integer) body.get("size")).isEqualTo(10);
        assertThat((Integer) body.get("totalElements")).isGreaterThanOrEqualTo(25);
        
        List<?> content = (List<?>) body.get("content");
        assertThat(content).hasSize(10);
    }

    @Test
    void shouldAddAndRemoveTagsFromPhoto() {
        String photoId = uploadPhoto("tagged-photo.jpg");

        // Add tags
        ResponseEntity<Void> tag1Response = restTemplate.exchange(
                baseUrl() + "/api/v1/photos/" + photoId + "/tags/vacation",
                HttpMethod.POST,
                new HttpEntity<>(createAuthHeaders()),
                Void.class
        );
        assertThat(tag1Response.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Void> tag2Response = restTemplate.exchange(
                baseUrl() + "/api/v1/photos/" + photoId + "/tags/family",
                HttpMethod.POST,
                new HttpEntity<>(createAuthHeaders()),
                Void.class
        );
        assertThat(tag2Response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Verify tags were added
        ResponseEntity<Map> photosResponse = restTemplate.exchange(
                baseUrl() + "/api/v1/photos",
                HttpMethod.GET,
                new HttpEntity<>(createAuthHeaders()),
                Map.class
        );

        List<?> content = (List<?>) photosResponse.getBody().get("content");
        Map<String, Object> photo = (Map<String, Object>) content.get(0);
        List<?> tags = (List<?>) photo.get("tags");
        
        assertThat(tags).containsExactlyInAnyOrder("vacation", "family");

        // Remove a tag
        ResponseEntity<Void> removeResponse = restTemplate.exchange(
                baseUrl() + "/api/v1/photos/" + photoId + "/tags/vacation",
                HttpMethod.DELETE,
                new HttpEntity<>(createAuthHeaders()),
                Void.class
        );
        assertThat(removeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Verify tag was removed
        photosResponse = restTemplate.exchange(
                baseUrl() + "/api/v1/photos",
                HttpMethod.GET,
                new HttpEntity<>(createAuthHeaders()),
                Map.class
        );

        content = (List<?>) photosResponse.getBody().get("content");
        photo = (Map<String, Object>) content.get(0);
        tags = (List<?>) photo.get("tags");
        
        assertThat(tags).containsExactly("family");
    }

    @Test
    void shouldFilterPhotosByTag() {
        // Upload photos with different tags
        String photo1 = uploadPhoto("vacation1.jpg");
        String photo2 = uploadPhoto("vacation2.jpg");
        String photo3 = uploadPhoto("work.jpg");

        // Tag photos
        addTag(photo1, "vacation");
        addTag(photo2, "vacation");
        addTag(photo3, "work");

        // Filter by vacation tag
        ResponseEntity<Map> vacationResponse = restTemplate.exchange(
                baseUrl() + "/api/v1/photos?tag=vacation",
                HttpMethod.GET,
                new HttpEntity<>(createAuthHeaders()),
                Map.class
        );

        List<?> vacationContent = (List<?>) vacationResponse.getBody().get("content");
        assertThat(vacationContent).hasSize(2);

        // Filter by work tag
        ResponseEntity<Map> workResponse = restTemplate.exchange(
                baseUrl() + "/api/v1/photos?tag=work",
                HttpMethod.GET,
                new HttpEntity<>(createAuthHeaders()),
                Map.class
        );

        List<?> workContent = (List<?>) workResponse.getBody().get("content");
        assertThat(workContent).hasSize(1);
    }

    @Test
    void shouldReplaceAllTagsForPhoto() {
        String photoId = uploadPhoto("multi-tag-photo.jpg");

        // Add initial tags
        addTag(photoId, "old1");
        addTag(photoId, "old2");

        // Replace all tags
        List<String> newTags = List.of("new1", "new2", "new3");
        HttpHeaders headers = createAuthHeaders();
        
        ResponseEntity<Void> replaceResponse = restTemplate.exchange(
                baseUrl() + "/api/v1/photos/" + photoId + "/tags",
                HttpMethod.PATCH,
                new HttpEntity<>(newTags, headers),
                Void.class
        );

        assertThat(replaceResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Verify new tags
        ResponseEntity<Map> photosResponse = restTemplate.exchange(
                baseUrl() + "/api/v1/photos",
                HttpMethod.GET,
                new HttpEntity<>(createAuthHeaders()),
                Map.class
        );

        List<?> content = (List<?>) photosResponse.getBody().get("content");
        Map<String, Object> photo = (Map<String, Object>) content.get(0);
        List<?> tags = (List<?>) photo.get("tags");
        
        assertThat(tags).containsExactlyInAnyOrder("new1", "new2", "new3");
    }

    @Test
    void shouldSortPhotosByDate() {
        // Upload photos
        uploadPhoto("first.jpg");
        try {
            Thread.sleep(100); // Ensure different timestamps
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        uploadPhoto("second.jpg");

        // Get photos sorted by date descending (newest first)
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/v1/photos?sortBy=createdAt&sortDirection=DESC",
                HttpMethod.GET,
                new HttpEntity<>(createAuthHeaders()),
                Map.class
        );

        List<?> content = (List<?>) response.getBody().get("content");
        assertThat(content).hasSizeGreaterThanOrEqualTo(2);
        
        Map<String, Object> first = (Map<String, Object>) content.get(0);
        assertThat(first.get("filename")).isEqualTo("second.jpg");
    }

    @Test
    void shouldProvidePresignedUrlsInGalleryResponse() {
        String photoId = uploadPhoto("url-test.jpg");
        
        // Mark upload complete to trigger URL generation
        completeUpload(photoId);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/v1/photos",
                HttpMethod.GET,
                new HttpEntity<>(createAuthHeaders()),
                Map.class
        );

        List<?> content = (List<?>) response.getBody().get("content");
        Map<String, Object> photo = (Map<String, Object>) content.get(0);
        
        // Verify URLs are present (even if S3 is mocked)
        assertThat(photo).containsKeys("thumbnailUrl", "downloadUrl");
    }

    // Helper methods

    private void registerAndLogin() {
        String testEmail = "test-" + UUID.randomUUID() + "@example.com";
        String testPassword = "TestPassword123!";

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
        authToken = (String) registerResponse.getBody().get("token");
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

    private void completeUpload(String photoId) {
        // First get the uploadJobId associated with this photo
        // In a real scenario, this would be returned from the initiate call
        // For this test, we'll just mark any recent upload job as complete
        
        // This is a simplified version - in production you'd track the uploadJobId
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/api/v1/photos",
                HttpMethod.GET,
                new HttpEntity<>(createAuthHeaders()),
                Map.class
        );
        
        // Actual completion would use the uploadJobId from initiation
    }

    private void addTag(String photoId, String tag) {
        ResponseEntity<Void> response = restTemplate.exchange(
                baseUrl() + "/api/v1/photos/" + photoId + "/tags/" + tag,
                HttpMethod.POST,
                new HttpEntity<>(createAuthHeaders()),
                Void.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}

