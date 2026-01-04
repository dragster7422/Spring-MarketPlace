package com.example.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class ImageValidationService {

    // Max file size: 5MB
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    // Max additional images count
    private static final int MAX_ADDITIONAL_IMAGES = 10;

    // Allowed image extensions
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "webp", "avif"
    );

    // Allowed MIME types
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/avif"
    );

    /**
     * Validate image file with detailed error messages
     *
     * @param file File to validate
     * @return ValidationResult with success status and error message
     */
    public ValidationResult validateImage(MultipartFile file) {
        // Check if file exists
        if (file == null || file.isEmpty()) {
            return ValidationResult.error("Image file is required");
        }

        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            return ValidationResult.error(
                    String.format("Image size exceeds maximum allowed size of %d MB", MAX_FILE_SIZE / (1024 * 1024))
            );
        }

        // Check filename exists
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            return ValidationResult.error("Invalid filename");
        }

        // Check file extension
        if (!hasValidExtension(filename)) {
            return ValidationResult.error(
                    "Invalid image format. Allowed formats: " + String.join(", ", ALLOWED_EXTENSIONS)
            );
        }

        // Check MIME type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            return ValidationResult.error(
                    "Invalid image type. Allowed types: " + String.join(", ", ALLOWED_MIME_TYPES)
            );
        }

        // Verify that file is actually an image by checking magic bytes
        try {
            if (!isValidImageFile(file)) {
                return ValidationResult.error("File is not a valid image");
            }
        } catch (IOException e) {
            log.error("Error reading file for validation: {}", filename, e);
            return ValidationResult.error("Error validating image file");
        }

        return ValidationResult.success();
    }

    /**
     * Validate preview image
     *
     * @param previewImage Preview image to validate
     * @return ValidationResult
     */
    public ValidationResult validatePreviewImage(MultipartFile previewImage) {
        if (previewImage == null || previewImage.isEmpty()) {
            return ValidationResult.error("Preview image is required");
        }
        return validateImage(previewImage);
    }

    /**
     * Validate list of additional images
     *
     * @param additionalImages List of additional images
     * @return ValidationResult
     */
    public ValidationResult validateAdditionalImages(List<MultipartFile> additionalImages) {
        if (additionalImages == null || additionalImages.isEmpty()) {
            return ValidationResult.success();
        }

        // Check count
        long nonEmptyCount = additionalImages.stream()
                .filter(file -> file != null && !file.isEmpty())
                .count();

        if (nonEmptyCount > MAX_ADDITIONAL_IMAGES) {
            return ValidationResult.error(
                    String.format("Maximum %d additional images allowed", MAX_ADDITIONAL_IMAGES)
            );
        }

        // Validate each image
        for (MultipartFile file : additionalImages) {
            if (file != null && !file.isEmpty()) {
                ValidationResult result = validateImage(file);
                if (!result.isValid()) {
                    return result;
                }
            }
        }

        return ValidationResult.success();
    }

    /**
     * Check if filename has valid extension
     *
     * @param filename Filename to check
     * @return true if extension is valid
     */
    private boolean hasValidExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return false;
        }

        String extension = filename.substring(lastDotIndex + 1).toLowerCase();
        return ALLOWED_EXTENSIONS.contains(extension);
    }

    /**
     * Verify file is a valid image by checking magic bytes (file signature)
     *
     * @param file File to check
     * @return true if file is a valid image
     * @throws IOException if error reading file
     */
    private boolean isValidImageFile(MultipartFile file) throws IOException {
        byte[] bytes = new byte[Math.min((int) file.getSize(), 20)];
        int bytesRead;

        try (InputStream inputStream = file.getInputStream()) {
            bytesRead = inputStream.read(bytes);
        }

        if (bytesRead < 2) {
            log.warn("File '{}' is too small: {} bytes read",
                    file.getOriginalFilename(), bytesRead);
            return false;
        }

        // Check magic bytes for different image formats
        return isJPEG(bytes) || isPNG(bytes) || isWEBP(bytes) || isAVIF(bytes);
    }

    /**
     * Check if bytes represent JPEG image
     * JPEG magic bytes: FF D8 FF
     */
    private boolean isJPEG(byte[] bytes) {
        return bytes.length >= 3 &&
                (bytes[0] & 0xFF) == 0xFF &&
                (bytes[1] & 0xFF) == 0xD8 &&
                (bytes[2] & 0xFF) == 0xFF;
    }

    /**
     * Check if bytes represent PNG image
     * PNG magic bytes: 89 50 4E 47 0D 0A 1A 0A
     */
    private boolean isPNG(byte[] bytes) {
        return bytes.length >= 8 &&
                (bytes[0] & 0xFF) == 0x89 &&
                bytes[1] == 0x50 &&
                bytes[2] == 0x4E &&
                bytes[3] == 0x47 &&
                (bytes[4] & 0xFF) == 0x0D &&
                (bytes[5] & 0xFF) == 0x0A &&
                (bytes[6] & 0xFF) == 0x1A &&
                (bytes[7] & 0xFF) == 0x0A;
    }

    /**
     * Check if bytes represent WEBP image
     * WEBP magic bytes: 52 49 46 46 ... 57 45 42 50 (RIFF...WEBP)
     */
    private boolean isWEBP(byte[] bytes) {
        return bytes.length >= 12 &&
                bytes[0] == 0x52 && // R
                bytes[1] == 0x49 && // I
                bytes[2] == 0x46 && // F
                bytes[3] == 0x46 && // F
                bytes[8] == 0x57 && // W
                bytes[9] == 0x45 && // E
                bytes[10] == 0x42 && // B
                bytes[11] == 0x50;  // P
    }

    /**
     * Check if bytes represent AVIF image
     * AVIF magic bytes:
     * - Bytes 4-7: "ftyp" (file type box)
     * - Bytes 8-11: "avif" or "avis" (brand)
     */
    private boolean isAVIF(byte[] bytes) {
        if (bytes.length < 12) {
            return false;
        }

        // Check for "ftyp" at bytes 4-7
        boolean hasFtyp = bytes[4] == 0x66 && // f
                bytes[5] == 0x74 && // t
                bytes[6] == 0x79 && // y
                bytes[7] == 0x70;   // p

        if (!hasFtyp) {
            return false;
        }

        // Check for "avif" or "avis" at bytes 8-11
        boolean isAvif = bytes[8] == 0x61 &&  // a
                bytes[9] == 0x76 &&  // v
                bytes[10] == 0x69 && // i
                (bytes[11] == 0x66 || bytes[11] == 0x73); // f or s

        return isAvif;
    }

    /**
     * Inner class for validation results
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult error(String message) {
            return new ValidationResult(false, message);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}