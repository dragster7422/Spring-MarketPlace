package com.example.services;

import com.example.elasticsearch.ProductDocument;
import com.example.models.Product;
import com.example.models.ProductImage;
import com.example.models.User;
import com.example.repositories.ProductRepository;
import com.example.repositories.ProductSearchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductSearchServiceTest {

    @Mock
    private ProductSearchRepository searchRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductSearchService productSearchService;

    private Product testProduct;
    private ProductDocument testDocument;
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

        // Add preview image to avoid NullPointerException
        ProductImage previewImage = new ProductImage();
        previewImage.setId(1L);
        previewImage.setImageDirectory("uploads/test-image.jpg");
        previewImage.setPreviewImage(true);
        testProduct.setImages(List.of(previewImage));

        testDocument = new ProductDocument(testProduct);
    }

    @Test
    void indexProduct_ShouldSaveProductDocument() {
        // Arrange
        when(searchRepository.save(any(ProductDocument.class))).thenReturn(testDocument);

        // Act
        productSearchService.indexProduct(testProduct);

        // Assert
        verify(searchRepository, times(1)).save(any(ProductDocument.class));
    }

    @Test
    void indexProduct_WhenExceptionThrown_ShouldHandleGracefully() {
        // Arrange
        when(searchRepository.save(any(ProductDocument.class))).thenThrow(new RuntimeException("ES error"));

        // Act & Assert
        assertDoesNotThrow(() -> productSearchService.indexProduct(testProduct));
        verify(searchRepository, times(1)).save(any(ProductDocument.class));
    }

    @Test
    void deleteProductFromIndex_ShouldDeleteById() {
        // Arrange
        doNothing().when(searchRepository).deleteById(anyLong());

        // Act
        productSearchService.deleteProductFromIndex(1L);

        // Assert
        verify(searchRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteProductFromIndex_WhenExceptionThrown_ShouldHandleGracefully() {
        // Arrange
        doThrow(new RuntimeException("ES error")).when(searchRepository).deleteById(anyLong());

        // Act & Assert
        assertDoesNotThrow(() -> productSearchService.deleteProductFromIndex(1L));
        verify(searchRepository, times(1)).deleteById(1L);
    }

    @Test
    void reindexAllProducts_ShouldDeleteAndSaveAll() {
        // Arrange
        List<Product> products = List.of(testProduct);
        when(productRepository.findAll()).thenReturn(products);
        doNothing().when(searchRepository).deleteAll();
        when(searchRepository.saveAll(anyList())).thenReturn(List.of(testDocument));

        // Act
        productSearchService.reindexAllProducts();

        // Assert
        verify(searchRepository, times(1)).deleteAll();
        verify(productRepository, times(1)).findAll();
        verify(searchRepository, times(1)).saveAll(anyList());
    }

    @Test
    void reindexAllProducts_WhenExceptionThrown_ShouldHandleGracefully() {
        // Arrange
        when(productRepository.findAll()).thenThrow(new RuntimeException("DB error"));

        // Act & Assert
        assertDoesNotThrow(() -> productSearchService.reindexAllProducts());
        verify(productRepository, times(1)).findAll();
    }

    @Test
    void searchProducts_WithQuery_ShouldReturnSearchResults() {
        // Arrange
        List<ProductDocument> documents = List.of(testDocument);
        Page<ProductDocument> documentPage = new PageImpl<>(documents);
        List<Product> products = List.of(testProduct);

        when(searchRepository.findByTitleContainingOrDescriptionContaining(
                anyString(), anyString(), any(Pageable.class))).thenReturn(documentPage);
        when(productRepository.findAllById(anyList())).thenReturn(products);

        // Act
        Page<Product> result = productSearchService.searchProducts("test", 0, 20);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(testProduct.getTitle(), result.getContent().get(0).getTitle());
        verify(searchRepository, times(1)).findByTitleContainingOrDescriptionContaining(
                eq("test"), eq("test"), any(Pageable.class));
        verify(productRepository, times(1)).findAllById(anyList());
    }

    @Test
    void searchProducts_WithEmptyQuery_ShouldReturnAllProducts() {
        // Arrange
        List<Product> products = List.of(testProduct);
        Page<Product> productPage = new PageImpl<>(products);
        when(productRepository.findAll(any(Pageable.class))).thenReturn(productPage);

        // Act
        Page<Product> result = productSearchService.searchProducts("", 0, 20);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(productRepository, times(1)).findAll(any(Pageable.class));
        verify(searchRepository, never()).findByTitleContainingOrDescriptionContaining(
                anyString(), anyString(), any(Pageable.class));
    }

    @Test
    void searchProducts_WithNullQuery_ShouldReturnAllProducts() {
        // Arrange
        List<Product> products = List.of(testProduct);
        Page<Product> productPage = new PageImpl<>(products);
        when(productRepository.findAll(any(Pageable.class))).thenReturn(productPage);

        // Act
        Page<Product> result = productSearchService.searchProducts(null, 0, 20);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(productRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    void searchProducts_WhenElasticsearchFails_ShouldFallbackToDatabase() {
        // Arrange
        List<Product> products = List.of(testProduct);
        Page<Product> productPage = new PageImpl<>(products);

        when(searchRepository.findByTitleContainingOrDescriptionContaining(
                anyString(), anyString(), any(Pageable.class))).thenThrow(new RuntimeException("ES error"));
        when(productRepository.findAll(any(Pageable.class))).thenReturn(productPage);

        // Act
        Page<Product> result = productSearchService.searchProducts("test", 0, 20);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(searchRepository, times(1)).findByTitleContainingOrDescriptionContaining(
                anyString(), anyString(), any(Pageable.class));
        verify(productRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    void searchProducts_WithMultipleResults_ShouldMaintainOrder() {
        // Arrange
        Product product2 = new Product();
        product2.setId(2L);
        product2.setTitle("Another Product");
        product2.setDescription("Another description");
        product2.setPrice(new BigDecimal("149.99"));
        product2.setOwner(testUser);

        // Add preview image to product2
        ProductImage previewImage2 = new ProductImage();
        previewImage2.setId(2L);
        previewImage2.setImageDirectory("uploads/test-image-2.jpg");
        previewImage2.setPreviewImage(true);
        product2.setImages(List.of(previewImage2));

        ProductDocument document2 = new ProductDocument(product2);

        List<ProductDocument> documents = List.of(testDocument, document2);
        Page<ProductDocument> documentPage = new PageImpl<>(documents);
        List<Product> products = List.of(testProduct, product2);

        when(searchRepository.findByTitleContainingOrDescriptionContaining(
                anyString(), anyString(), any(Pageable.class))).thenReturn(documentPage);
        when(productRepository.findAllById(anyList())).thenReturn(products);

        // Act
        Page<Product> result = productSearchService.searchProducts("product", 0, 20);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        verify(searchRepository, times(1)).findByTitleContainingOrDescriptionContaining(
                eq("product"), eq("product"), any(Pageable.class));
    }
}