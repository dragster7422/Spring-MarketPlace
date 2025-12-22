package com.example.services;

import com.example.models.ProductImage;
import com.example.repositories.ProductImageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductImageServiceTest {

    @Mock
    private ProductImageRepository productImageRepository;

    @InjectMocks
    private ProductImageService productImageService;

    private ProductImage testImage;

    @BeforeEach
    void setUp() {
        testImage = new ProductImage();
        testImage.setId(1L);
        testImage.setImageDirectory("uploads/test-image.jpg");
        testImage.setPreviewImage(true);
    }

    @Test
    void getImageById_WhenImageExists_ShouldReturnImage() {
        // Arrange
        when(productImageRepository.findById(anyLong())).thenReturn(Optional.of(testImage));

        // Act
        ProductImage result = productImageService.getImageById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(testImage.getId(), result.getId());
        assertEquals(testImage.getImageDirectory(), result.getImageDirectory());
        verify(productImageRepository, times(1)).findById(1L);
    }

    @Test
    void getImageById_WhenImageDoesNotExist_ShouldReturnNull() {
        // Arrange
        when(productImageRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act
        ProductImage result = productImageService.getImageById(999L);

        // Assert
        assertNull(result);
        verify(productImageRepository, times(1)).findById(999L);
    }

    @Test
    void deleteImageById_ShouldCallRepository() {
        // Arrange
        doNothing().when(productImageRepository).deleteById(anyLong());

        // Act
        productImageService.deleteImageById(1L);

        // Assert
        verify(productImageRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteImageFromDisk_WithNullImage_ShouldNotThrowException() {
        // Act & Assert
        assertDoesNotThrow(() -> productImageService.deleteImageFromDisk(null));
    }

    @Test
    void deleteImageFromDisk_WithNullImageDirectory_ShouldNotThrowException() {
        // Arrange
        testImage.setImageDirectory(null);

        // Act & Assert
        assertDoesNotThrow(() -> productImageService.deleteImageFromDisk(testImage));
    }

    @Test
    void deleteImageFromDisk_WithValidImage_ShouldAttemptDeletion() {
        // This test verifies that the method executes without exceptions
        // In a real scenario, you might need to create actual test files

        // Act & Assert
        assertDoesNotThrow(() -> productImageService.deleteImageFromDisk(testImage));
    }
}