package com.rapidphoto.uploader.infrastructure.storage;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Utility for generating organized storage keys for uploaded files.
 * Pattern: {userId}/{year}/{month}/{day}/{uuid}-{filename}
 */
@Component
public class StorageKeyGenerator {

    private static final DateTimeFormatter YEAR_FORMATTER = DateTimeFormatter.ofPattern("yyyy");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MM");
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("dd");

    /**
     * Generate a unique storage key for a file.
     * @param userId the user's ID
     * @param filename the original filename
     * @return organized storage key
     */
    public String generateKey(UUID userId, String filename) {
        LocalDate now = LocalDate.now();
        String year = now.format(YEAR_FORMATTER);
        String month = now.format(MONTH_FORMATTER);
        String day = now.format(DAY_FORMATTER);
        String uniqueId = UUID.randomUUID().toString();
        
        // Sanitize filename
        String sanitizedFilename = sanitizeFilename(filename);
        
        return String.format("%s/%s/%s/%s/%s-%s",
                userId, year, month, day, uniqueId, sanitizedFilename);
    }

    /**
     * Sanitize filename to remove potentially problematic characters.
     * @param filename the original filename
     * @return sanitized filename
     */
    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "unnamed";
        }
        
        // Remove or replace problematic characters
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}

