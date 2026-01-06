package com.example.services;

import com.example.services.ImageValidationService.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ImageValidationServiceTest {

    private ImageValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new ImageValidationService();
    }

    @Nested
    @DisplayName("validateImage() tests")
    class ValidateImageTests {

        @Test
        @DisplayName("Should fail when file is null")
        void shouldFailWhenFileIsNull() {
            // When
            ValidationResult result = validationService.validateImage(null);

            // Then
            assertFalse(result.isValid());
            assertEquals("Image file is required", result.getErrorMessage());
        }

        @Test
        @DisplayName("Should fail when file is empty")
        void shouldFailWhenFileIsEmpty() {
            // Given
            MockMultipartFile emptyFile = new MockMultipartFile(
                    "file",
                    "test.jpg",
                    "image/jpeg",
                    new byte[0]
            );

            // When
            ValidationResult result = validationService.validateImage(emptyFile);

            // Then
            assertFalse(result.isValid());
            assertEquals("Image file is required", result.getErrorMessage());
        }

        @Test
        @DisplayName("Should fail when file size is zero")
        void shouldFailWhenFileSizeIsZero() {
            // Given
            MockMultipartFile zeroSizeFile = new MockMultipartFile(
                    "file",
                    "test.jpg",
                    "image/jpeg",
                    new byte[0]
            );

            // When
            ValidationResult result = validationService.validateImage(zeroSizeFile);

            // Then
            assertFalse(result.isValid());
            assertEquals("Image file is required", result.getErrorMessage());
        }

        @Test
        @DisplayName("Should fail when file exceeds max size (6MB)")
        void shouldFailWhenFileSizeExceedsMaximum() {
            // Given - 6MB file
            byte[] largeContent = new byte[7 * 1024 * 1024];
            MockMultipartFile largeFile = new MockMultipartFile(
                    "file",
                    "large.jpg",
                    "image/jpeg",
                    largeContent
            );

            // When
            ValidationResult result = validationService.validateImage(largeFile);

            // Then
            assertFalse(result.isValid());
            assertTrue(result.getErrorMessage().contains("exceeds maximum allowed size"));
        }

        @Test
        @DisplayName("Should fail when filename is null")
        void shouldFailWhenFilenameIsNull() {
            // Given
            MockMultipartFile fileWithoutName = new MockMultipartFile(
                    "file",
                    null,
                    "image/jpeg",
                    createValidJpegBytes()
            );

            // When
            ValidationResult result = validationService.validateImage(fileWithoutName);

            // Then
            assertFalse(result.isValid());
            assertEquals("Invalid filename", result.getErrorMessage());
        }

        @Test
        @DisplayName("Should fail when filename is blank")
        void shouldFailWhenFilenameIsBlank() {
            // Given
            MockMultipartFile fileWithBlankName = new MockMultipartFile(
                    "file",
                    "   ",
                    "image/jpeg",
                    createValidJpegBytes()
            );

            // When
            ValidationResult result = validationService.validateImage(fileWithBlankName);

            // Then
            assertFalse(result.isValid());
            assertEquals("Invalid filename", result.getErrorMessage());
        }

        @Test
        @DisplayName("Should fail when file extension is invalid")
        void shouldFailWhenExtensionIsInvalid() {
            // Given
            MockMultipartFile invalidExtensionFile = new MockMultipartFile(
                    "file",
                    "test.txt",
                    "text/plain",
                    createValidJpegBytes()
            );

            // When
            ValidationResult result = validationService.validateImage(invalidExtensionFile);

            // Then
            assertFalse(result.isValid());
            assertTrue(result.getErrorMessage().contains("Invalid image format"));
        }

        @Test
        @DisplayName("Should fail when MIME type is null")
        void shouldFailWhenMimeTypeIsNull() {
            // Given
            MockMultipartFile fileWithoutMimeType = new MockMultipartFile(
                    "file",
                    "test.jpg",
                    null,
                    createValidJpegBytes()
            );

            // When
            ValidationResult result = validationService.validateImage(fileWithoutMimeType);

            // Then
            assertFalse(result.isValid());
            assertTrue(result.getErrorMessage().contains("Invalid image type"));
        }

        @Test
        @DisplayName("Should fail when MIME type is invalid")
        void shouldFailWhenMimeTypeIsInvalid() {
            // Given
            MockMultipartFile invalidMimeTypeFile = new MockMultipartFile(
                    "file",
                    "test.jpg",
                    "application/pdf",
                    createValidJpegBytes()
            );

            // When
            ValidationResult result = validationService.validateImage(invalidMimeTypeFile);

            // Then
            assertFalse(result.isValid());
            assertTrue(result.getErrorMessage().contains("Invalid image type"));
        }

        @Test
        @DisplayName("Should fail when magic bytes are invalid (fake JPEG)")
        void shouldFailWhenMagicBytesAreInvalid() {
            // Given - file with .jpg extension and MIME type but invalid magic bytes
            byte[] fakeJpeg = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00};
            MockMultipartFile fakeFile = new MockMultipartFile(
                    "file",
                    "fake.jpg",
                    "image/jpeg",
                    fakeJpeg
            );

            // When
            ValidationResult result = validationService.validateImage(fakeFile);

            // Then
            assertFalse(result.isValid());
            assertEquals("File is not a valid image", result.getErrorMessage());
        }

        @Test
        @DisplayName("Should succeed for valid JPEG file")
        void shouldSucceedForValidJpeg() {
            // Given
            MockMultipartFile validJpeg = new MockMultipartFile(
                    "file",
                    "test.jpg",
                    "image/jpeg",
                    createValidJpegBytes()
            );

            // When
            ValidationResult result = validationService.validateImage(validJpeg);

            // Then
            assertTrue(result.isValid());
            assertNull(result.getErrorMessage());
        }

        @Test
        @DisplayName("Should succeed for valid PNG file")
        void shouldSucceedForValidPng() {
            // Given
            MockMultipartFile validPng = new MockMultipartFile(
                    "file",
                    "test.png",
                    "image/png",
                    createValidPngBytes()
            );

            // When
            ValidationResult result = validationService.validateImage(validPng);

            // Then
            assertTrue(result.isValid());
            assertNull(result.getErrorMessage());
        }

        @Test
        @DisplayName("Should succeed for valid WebP file")
        void shouldSucceedForValidWebp() {
            // Given
            MockMultipartFile validWebp = new MockMultipartFile(
                    "file",
                    "test.webp",
                    "image/webp",
                    createValidWebpBytes()
            );

            // When
            ValidationResult result = validationService.validateImage(validWebp);

            // Then
            assertTrue(result.isValid());
            assertNull(result.getErrorMessage());
        }

        @Test
        @DisplayName("Should succeed for valid AVIF file")
        void shouldSucceedForValidAvif() {
            // Given
            MockMultipartFile validAvif = new MockMultipartFile(
                    "file",
                    "test.avif",
                    "image/avif",
                    createValidAvifBytes()
            );

            // When
            ValidationResult result = validationService.validateImage(validAvif);

            // Then
            assertTrue(result.isValid());
            assertNull(result.getErrorMessage());
        }

        @Test
        @DisplayName("Should accept file at exactly 5MB limit")
        void shouldAcceptFileAtExactLimit() {
            // Given - exactly 5MB
            byte[] maxSizeContent = new byte[5 * 1024 * 1024];
            // Add JPEG magic bytes
            maxSizeContent[0] = (byte) 0xFF;
            maxSizeContent[1] = (byte) 0xD8;
            maxSizeContent[2] = (byte) 0xFF;

            MockMultipartFile maxSizeFile = new MockMultipartFile(
                    "file",
                    "max.jpg",
                    "image/jpeg",
                    maxSizeContent
            );

            // When
            ValidationResult result = validationService.validateImage(maxSizeFile);

            // Then
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("Should handle file with uppercase extension")
        void shouldHandleUppercaseExtension() {
            // Given
            MockMultipartFile uppercaseFile = new MockMultipartFile(
                    "file",
                    "test.JPG",
                    "image/jpeg",
                    createValidJpegBytes()
            );

            // When
            ValidationResult result = validationService.validateImage(uppercaseFile);

            // Then
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("Should handle file with mixed case MIME type")
        void shouldHandleMixedCaseMimeType() {
            // Given
            MockMultipartFile mixedCaseFile = new MockMultipartFile(
                    "file",
                    "test.jpg",
                    "IMAGE/JPEG",
                    createValidJpegBytes()
            );

            // When
            ValidationResult result = validationService.validateImage(mixedCaseFile);

            // Then
            assertTrue(result.isValid());
        }
    }

    @Nested
    @DisplayName("validatePreviewImage() tests")
    class ValidatePreviewImageTests {

        @Test
        @DisplayName("Should fail when preview image is null")
        void shouldFailWhenPreviewImageIsNull() {
            // When
            ValidationResult result = validationService.validatePreviewImage(null);

            // Then
            assertFalse(result.isValid());
            assertEquals("Preview image is required", result.getErrorMessage());
        }

        @Test
        @DisplayName("Should fail when preview image is empty")
        void shouldFailWhenPreviewImageIsEmpty() {
            // Given
            MockMultipartFile emptyFile = new MockMultipartFile(
                    "file",
                    "test.jpg",
                    "image/jpeg",
                    new byte[0]
            );

            // When
            ValidationResult result = validationService.validatePreviewImage(emptyFile);

            // Then
            assertFalse(result.isValid());
            assertEquals("Preview image is required", result.getErrorMessage());
        }

        @Test
        @DisplayName("Should succeed for valid preview image")
        void shouldSucceedForValidPreviewImage() {
            // Given
            MockMultipartFile validPreview = new MockMultipartFile(
                    "file",
                    "preview.jpg",
                    "image/jpeg",
                    createValidJpegBytes()
            );

            // When
            ValidationResult result = validationService.validatePreviewImage(validPreview);

            // Then
            assertTrue(result.isValid());
        }
    }

    @Nested
    @DisplayName("validateAdditionalImages() tests")
    class ValidateAdditionalImagesTests {

        @Test
        @DisplayName("Should succeed when additional images list is null")
        void shouldSucceedWhenListIsNull() {
            // When
            ValidationResult result = validationService.validateAdditionalImages(null);

            // Then
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("Should succeed when additional images list is empty")
        void shouldSucceedWhenListIsEmpty() {
            // When
            ValidationResult result = validationService.validateAdditionalImages(Collections.emptyList());

            // Then
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("Should succeed for valid additional images (less than 10)")
        void shouldSucceedForValidAdditionalImages() {
            // Given
            List<MultipartFile> images = Arrays.asList(
                    createValidJpegFile("image1.jpg"),
                    createValidPngFile("image2.png")
            );

            // When
            ValidationResult result = validationService.validateAdditionalImages(images);

            // Then
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("Should succeed for exactly 10 additional images")
        void shouldSucceedForExactly10Images() {
            // Given - exactly 10 images
            List<MultipartFile> images = Arrays.asList(
                    createValidJpegFile("1.jpg"),
                    createValidJpegFile("2.jpg"),
                    createValidJpegFile("3.jpg"),
                    createValidJpegFile("4.jpg"),
                    createValidJpegFile("5.jpg"),
                    createValidJpegFile("6.jpg"),
                    createValidJpegFile("7.jpg"),
                    createValidJpegFile("8.jpg"),
                    createValidJpegFile("9.jpg"),
                    createValidJpegFile("10.jpg")
            );

            // When
            ValidationResult result = validationService.validateAdditionalImages(images);

            // Then
            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("Should fail when more than 10 additional images")
        void shouldFailWhenMoreThan10Images() {
            // Given - 11 images
            List<MultipartFile> images = Arrays.asList(
                    createValidJpegFile("1.jpg"),
                    createValidJpegFile("2.jpg"),
                    createValidJpegFile("3.jpg"),
                    createValidJpegFile("4.jpg"),
                    createValidJpegFile("5.jpg"),
                    createValidJpegFile("6.jpg"),
                    createValidJpegFile("7.jpg"),
                    createValidJpegFile("8.jpg"),
                    createValidJpegFile("9.jpg"),
                    createValidJpegFile("10.jpg"),
                    createValidJpegFile("11.jpg")
            );

            // When
            ValidationResult result = validationService.validateAdditionalImages(images);

            // Then
            assertFalse(result.isValid());
            assertTrue(result.getErrorMessage().contains("Maximum 10 additional images allowed"));
        }

        @Test
        @DisplayName("Should fail when one image in list is invalid")
        void shouldFailWhenOneImageIsInvalid() {
            // Given
            MockMultipartFile invalidFile = new MockMultipartFile(
                    "file",
                    "invalid.txt",
                    "text/plain",
                    new byte[]{0x00, 0x00, 0x00}
            );

            List<MultipartFile> images = Arrays.asList(
                    createValidJpegFile("valid.jpg"),
                    invalidFile
            );

            // When
            ValidationResult result = validationService.validateAdditionalImages(images);

            // Then
            assertFalse(result.isValid());
        }

        @Test
        @DisplayName("Should skip null and empty files in list when counting")
        void shouldSkipNullAndEmptyFilesWhenCounting() {
            // Given - 3 valid files + 2 empty + 1 null = should count only 3
            List<MultipartFile> images = Arrays.asList(
                    createValidJpegFile("1.jpg"),
                    new MockMultipartFile("empty", "", "image/jpeg", new byte[0]),
                    createValidJpegFile("2.jpg"),
                    null,
                    createValidJpegFile("3.jpg")
            );

            // When
            ValidationResult result = validationService.validateAdditionalImages(images);

            // Then
            assertTrue(result.isValid());
        }
    }

    // ========================================
    // Helper methods to create valid image bytes
    // ========================================

    private byte[] createValidJpegBytes() {
        return new byte[]{
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
                0x00, 0x10, 0x4A, 0x46, 0x49, 0x46
        };
    }

    private byte[] createValidPngBytes() {
        return new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47,
                0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00
        };
    }

    private byte[] createValidWebpBytes() {
        return new byte[]{
                0x52, 0x49, 0x46, 0x46,  // "RIFF"
                0x00, 0x00, 0x00, 0x00,  // file size
                0x57, 0x45, 0x42, 0x50,  // "WEBP"
                0x00, 0x00
        };
    }

    private byte[] createValidAvifBytes() {
        return new byte[]{
                0x00, 0x00, 0x00, 0x20,  // box size
                0x66, 0x74, 0x79, 0x70,  // "ftyp"
                0x61, 0x76, 0x69, 0x66,  // "avif"
                0x00, 0x00, 0x00, 0x00
        };
    }

    private MockMultipartFile createValidJpegFile(String filename) {
        return new MockMultipartFile(
                "file",
                filename,
                "image/jpeg",
                createValidJpegBytes()
        );
    }

    private MockMultipartFile createValidPngFile(String filename) {
        return new MockMultipartFile(
                "file",
                filename,
                "image/png",
                createValidPngBytes()
        );
    }
}