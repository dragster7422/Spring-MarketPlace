package com.example.services;

import com.example.models.Product;
import com.example.models.ProductImage;
import com.example.models.User;
import com.example.repositories.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductImageService productImageService;

    @Mock
    private ProductSearchService searchService;

    @InjectMocks
    private ProductService productService;

    private Product testProduct;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setTitle("Test Product");
        testProduct.setDescription("Test Description for the product");
        testProduct.setPrice(new BigDecimal("99.99"));
        testProduct.setOwner(testUser);
        testProduct.setImages(new ArrayList<>());
    }

    @Test
    void getProducts_ShouldReturnPageOfProducts() {
        // Arrange
        List<Product> products = List.of(testProduct);
        Page<Product> productPage = new PageImpl<>(products);
        when(productRepository.findAll(any(Pageable.class))).thenReturn(productPage);

        // Act
        Page<Product> result = productService.getProducts(0, 20);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(testProduct.getTitle(), result.getContent().get(0).getTitle());
        verify(productRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    void getById_WhenProductExists_ShouldReturnProduct() {
        // Arrange
        when(productRepository.findById(anyLong())).thenReturn(Optional.of(testProduct));

        // Act
        Product result = productService.getById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(testProduct.getId(), result.getId());
        assertEquals(testProduct.getTitle(), result.getTitle());
        verify(productRepository, times(1)).findById(1L);
    }

    @Test
    void getById_WhenProductDoesNotExist_ShouldReturnNull() {
        // Arrange
        when(productRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act
        Product result = productService.getById(999L);

        // Assert
        assertNull(result);
        verify(productRepository, times(1)).findById(999L);
    }

    @Test
    void save_ShouldSaveProductAndIndexIt() {
        // Arrange
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);
        doNothing().when(searchService).indexProduct(any(Product.class));

        // Act
        productService.save(testProduct);

        // Assert
        verify(productRepository, times(1)).save(testProduct);
        verify(searchService, times(1)).indexProduct(testProduct);
    }

    @Test
    void getProductsByOwnerId_ShouldReturnOwnerProducts() {
        // Arrange
        List<Product> products = List.of(testProduct);
        when(productRepository.findByOwnerId(anyLong())).thenReturn(products);

        // Act
        List<Product> result = productService.getProductsByOwnerId(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testProduct.getTitle(), result.get(0).getTitle());
        verify(productRepository, times(1)).findByOwnerId(1L);
    }

    @Test
    void isOwner_WhenUserIsOwner_ShouldReturnTrue() {
        // Act
        boolean result = productService.isOwner(testProduct, testUser);

        // Assert
        assertTrue(result);
    }

    @Test
    void isOwner_WhenUserIsNotOwner_ShouldReturnFalse() {
        // Arrange
        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setUsername("otheruser");

        // Act
        boolean result = productService.isOwner(testProduct, otherUser);

        // Assert
        assertFalse(result);
    }

    @Test
    void isOwner_WhenProductIsNull_ShouldReturnFalse() {
        // Act
        boolean result = productService.isOwner(null, testUser);

        // Assert
        assertFalse(result);
    }

    @Test
    void isOwner_WhenUserIsNull_ShouldReturnFalse() {
        // Act
        boolean result = productService.isOwner(testProduct, null);

        // Assert
        assertFalse(result);
    }

    @Test
    void deleteProductById_ShouldDeleteProductAndImages() {
        // Arrange
        ProductImage image1 = new ProductImage();
        ProductImage image2 = new ProductImage();
        testProduct.setImages(List.of(image1, image2));

        when(productRepository.findById(anyLong())).thenReturn(Optional.of(testProduct));
        doNothing().when(productImageService).deleteImageFromDisk(any(ProductImage.class));
        doNothing().when(productRepository).deleteById(anyLong());
        doNothing().when(searchService).deleteProductFromIndex(anyLong());

        // Act
        productService.deleteProductById(1L);

        // Assert
        verify(productRepository, times(1)).findById(1L);
        verify(productImageService, times(2)).deleteImageFromDisk(any(ProductImage.class));
        verify(productRepository, times(1)).deleteById(1L);
        verify(searchService, times(1)).deleteProductFromIndex(1L);
    }

    @Test
    void deleteProductById_WhenProductNotFound_ShouldNotDelete() {
        // Arrange
        when(productRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act
        productService.deleteProductById(999L);

        // Assert
        verify(productRepository, times(1)).findById(999L);
        verify(productRepository, never()).deleteById(anyLong());
        verify(searchService, never()).deleteProductFromIndex(anyLong());
    }

    @Test
    void searchProducts_ShouldDelegateToSearchService() {
        // Arrange
        List<Product> products = List.of(testProduct);
        Page<Product> productPage = new PageImpl<>(products);
        when(searchService.searchProducts(anyString(), anyInt(), anyInt())).thenReturn(productPage);

        // Act
        Page<Product> result = productService.searchProducts("test", 0, 20);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(searchService, times(1)).searchProducts("test", 0, 20);
    }

    @Test
    void updateProduct_WithValidData_ShouldReturnTrue() {
        // Arrange
        Product updatedProduct = new Product();
        updatedProduct.setTitle("Updated Title");
        updatedProduct.setDescription("Updated Description that is long enough");
        updatedProduct.setPrice(new BigDecimal("199.99"));

        when(productRepository.findById(anyLong())).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);
        doNothing().when(searchService).indexProduct(any(Product.class));

        // Act
        boolean result = productService.updateProduct(1L, null, null, null, updatedProduct);

        // Assert
        assertTrue(result);
        assertEquals("Updated Title", testProduct.getTitle());
        assertEquals("Updated Description that is long enough", testProduct.getDescription());
        assertEquals(new BigDecimal("199.99"), testProduct.getPrice());
        verify(productRepository, times(1)).findById(1L);
        verify(productRepository, times(1)).save(testProduct);
        verify(searchService, times(1)).indexProduct(testProduct);
    }

    @Test
    void updateProduct_WhenProductNotFound_ShouldReturnFalse() {
        // Arrange
        Product updatedProduct = new Product();
        when(productRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act
        boolean result = productService.updateProduct(999L, null, null, null, updatedProduct);

        // Assert
        assertFalse(result);
        verify(productRepository, times(1)).findById(999L);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void updateProduct_WithRemoveImages_ShouldRemoveImages() {
        // Arrange
        ProductImage image1 = new ProductImage();
        image1.setId(1L);
        ProductImage image2 = new ProductImage();
        image2.setId(2L);
        testProduct.setImages(new ArrayList<>(List.of(image1, image2)));

        Product updatedProduct = new Product();
        updatedProduct.setTitle("Updated Title");
        updatedProduct.setDescription("Updated Description that is long enough");
        updatedProduct.setPrice(new BigDecimal("199.99"));

        when(productRepository.findById(anyLong())).thenReturn(Optional.of(testProduct));
        when(productImageService.getImageById(anyLong())).thenReturn(image1);
        doNothing().when(productImageService).deleteImageFromDisk(any(ProductImage.class));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);
        doNothing().when(searchService).indexProduct(any(Product.class));

        List<Long> removeImageIds = List.of(1L);

        // Act
        boolean result = productService.updateProduct(1L, null, null, removeImageIds, updatedProduct);

        // Assert
        assertTrue(result);
        verify(productImageService, times(1)).getImageById(1L);
        verify(productImageService, times(1)).deleteImageFromDisk(image1);
        verify(productRepository, times(1)).save(testProduct);
    }

    @Test
    void saveProductWithImages_WithNullPreviewImage_ShouldReturnFalse() throws Exception {
        // Act
        boolean result = productService.saveProductWithImages(null, null, testProduct, testUser);

        // Assert
        assertFalse(result);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void saveProductWithImages_WithEmptyPreviewImage_ShouldReturnFalse() throws Exception {
        // Arrange
        MultipartFile emptyFile = new MockMultipartFile("file", new byte[0]);

        // Act
        boolean result = productService.saveProductWithImages(emptyFile, null, testProduct, testUser);

        // Assert
        assertFalse(result);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void saveProductWithImages_WithNullProduct_ShouldReturnFalse() throws Exception {
        // Arrange
        MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test".getBytes());

        // Act
        boolean result = productService.saveProductWithImages(file, null, null, testUser);

        // Assert
        assertFalse(result);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void saveProductWithImages_WithNullOwner_ShouldReturnFalse() throws Exception {
        // Arrange
        MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test".getBytes());

        // Act
        boolean result = productService.saveProductWithImages(file, null, testProduct, null);

        // Assert
        assertFalse(result);
        verify(productRepository, never()).save(any(Product.class));
    }
}