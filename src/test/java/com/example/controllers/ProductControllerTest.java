package com.example.controllers;

import com.example.configurations.SecurityConfig;
import com.example.models.Product;
import com.example.models.ProductImage;
import com.example.models.User;
import com.example.models.enums.Role;
import com.example.services.ProductService;
import com.example.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@Import(SecurityConfig.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @MockBean
    private UserService userService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    private Product testProduct;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.getRoles().add(Role.ROLE_USER);

        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setTitle("Test Product");
        testProduct.setDescription("Test Description for the product");
        testProduct.setPrice(new BigDecimal("99.99"));
        testProduct.setOwner(testUser);

        // Add preview image
        ProductImage previewImage = new ProductImage();
        previewImage.setId(1L);
        previewImage.setImageDirectory("uploads/test-image.jpg");
        previewImage.setPreviewImage(true);
        testProduct.setImages(List.of(previewImage));
    }

    @Test
    void products_ShouldReturnProductsPage() throws Exception {
        // Arrange
        List<Product> products = List.of(testProduct);
        Page<Product> productPage = new PageImpl<>(products);
        when(productService.getProducts(anyInt(), anyInt())).thenReturn(productPage);

        // Act & Assert
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("products"))
                .andExpect(model().attributeExists("products"))
                .andExpect(model().attributeExists("currentPage"))
                .andExpect(model().attributeExists("totalPages"));

        verify(productService, times(1)).getProducts(0, 20);
    }

    @Test
    void products_WithSearchQuery_ShouldReturnSearchResults() throws Exception {
        // Arrange
        List<Product> products = List.of(testProduct);
        Page<Product> productPage = new PageImpl<>(products);
        when(productService.searchProducts(anyString(), anyInt(), anyInt())).thenReturn(productPage);

        // Act & Assert
        mockMvc.perform(get("/").param("query", "test"))
                .andExpect(status().isOk())
                .andExpect(view().name("products"))
                .andExpect(model().attribute("searchQuery", "test"));

        verify(productService, times(1)).searchProducts("test", 0, 20);
    }

    @Test
    @WithMockUser
    void addProduct_GET_ShouldReturnAddProductPage() throws Exception {
        mockMvc.perform(get("/product/add"))
                .andExpect(status().isOk())
                .andExpect(view().name("product-add"))
                .andExpect(model().attributeExists("product"));
    }

    @Test
    @WithMockUser
    void addProduct_POST_WithValidData_ShouldRedirectToMyProducts() throws Exception {
        // Arrange
        MockMultipartFile previewImage = new MockMultipartFile(
                "previewImage", "test.jpg", "image/jpeg", "test".getBytes());

        when(productService.saveProductWithImages(any(), any(), any(), any())).thenReturn(true);

        // Act & Assert
        mockMvc.perform(multipart("/product/add")
                        .file(previewImage)
                        .with(csrf())
                        .with(user(testUser))
                        .param("title", "New Product")
                        .param("description", "This is a long description for the product")
                        .param("price", "99.99"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile/products"));

        verify(productService, times(1)).saveProductWithImages(any(), any(), any(), any());
    }

    @Test
    @WithMockUser
    void addProduct_POST_WithoutPreviewImage_ShouldReturnError() throws Exception {
        // Create an empty multipart file to simulate missing preview image
        MockMultipartFile emptyPreviewImage = new MockMultipartFile(
                "previewImage", "", "image/jpeg", new byte[0]);

        // Act & Assert
        mockMvc.perform(multipart("/product/add")
                        .file(emptyPreviewImage)
                        .with(csrf())
                        .with(user(testUser))
                        .param("title", "New Product")
                        .param("description", "This is a long description for the product")
                        .param("price", "99.99"))
                .andExpect(status().isOk())
                .andExpect(view().name("product-add"));

        verify(productService, never()).saveProductWithImages(any(), any(), any(), any());
    }

    @Test
    @WithMockUser
    void addProduct_POST_WithInvalidData_ShouldReturnValidationErrors() throws Exception {
        // Arrange
        MockMultipartFile previewImage = new MockMultipartFile(
                "previewImage", "test.jpg", "image/jpeg", "test".getBytes());

        // Act & Assert
        mockMvc.perform(multipart("/product/add")
                        .file(previewImage)
                        .with(csrf())
                        .with(user(testUser))
                        .param("title", "")
                        .param("description", "Short")
                        .param("price", "99.99"))
                .andExpect(status().isOk())
                .andExpect(view().name("product-add"))
                .andExpect(model().hasErrors());

        verify(productService, never()).saveProductWithImages(any(), any(), any(), any());
    }

    @Test
    void productDetails_ShouldReturnDetailsPage() throws Exception {
        // Arrange
        when(productService.getById(anyLong())).thenReturn(testProduct);

        // Act & Assert
        mockMvc.perform(get("/product/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("product-details"))
                .andExpect(model().attributeExists("product"))
                .andExpect(model().attributeExists("isOwner"));

        verify(productService, times(1)).getById(1L);
    }

    @Test
    void productDetails_WhenProductNotFound_ShouldRedirect() throws Exception {
        // Arrange
        when(productService.getById(anyLong())).thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/product/999"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        verify(productService, times(1)).getById(999L);
    }

    @Test
    @WithMockUser
    void productEdit_GET_AsOwner_ShouldReturnEditPage() throws Exception {
        // Arrange
        when(productService.getById(anyLong())).thenReturn(testProduct);
        when(productService.isOwner(any(), any())).thenReturn(true);

        // Act & Assert
        mockMvc.perform(get("/product/1/edit")
                        .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("product-edit"))
                .andExpect(model().attributeExists("product"));

        verify(productService, times(1)).getById(1L);
        verify(productService, times(1)).isOwner(any(), any());
    }

    @Test
    @WithMockUser
    void productEdit_GET_AsNonOwner_ShouldRedirect() throws Exception {
        // Arrange
        when(productService.getById(anyLong())).thenReturn(testProduct);
        when(productService.isOwner(any(), any())).thenReturn(false);

        // Act & Assert
        mockMvc.perform(get("/product/1/edit")
                        .with(user(testUser)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/product/1"));

        verify(productService, times(1)).getById(1L);
        verify(productService, times(1)).isOwner(any(), any());
    }

    @Test
    @WithMockUser
    void productEdit_POST_WithValidData_ShouldRedirectToDetails() throws Exception {
        // Arrange
        when(productService.getById(anyLong())).thenReturn(testProduct);
        when(productService.isOwner(any(), any())).thenReturn(true);
        when(productService.updateProduct(anyLong(), any(), any(), any(), any())).thenReturn(true);

        // Act & Assert
        mockMvc.perform(multipart("/product/1/edit")
                        .with(csrf())
                        .with(user(testUser))
                        .param("title", "Updated Product")
                        .param("description", "Updated description for the product")
                        .param("price", "149.99"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/product/1"));

        verify(productService, times(1)).updateProduct(anyLong(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser
    void productDelete_AsOwner_ShouldDeleteAndRedirect() throws Exception {
        // Arrange
        when(productService.getById(anyLong())).thenReturn(testProduct);
        when(productService.isOwner(any(), any())).thenReturn(true);
        doNothing().when(productService).deleteProductById(anyLong());

        // Act & Assert
        mockMvc.perform(post("/product/1/delete")
                        .with(csrf())
                        .with(user(testUser)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile/products"));

        verify(productService, times(1)).deleteProductById(1L);
    }

    @Test
    @WithMockUser
    void productDelete_AsNonOwner_ShouldNotDelete() throws Exception {
        // Arrange
        when(productService.getById(anyLong())).thenReturn(testProduct);
        when(productService.isOwner(any(), any())).thenReturn(false);

        // Act & Assert
        mockMvc.perform(post("/product/1/delete")
                        .with(csrf())
                        .with(user(testUser)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/product/1"));

        verify(productService, never()).deleteProductById(anyLong());
    }

    @Test
    @WithMockUser
    void productDelete_WhenProductNotFound_ShouldRedirect() throws Exception {
        // Arrange
        when(productService.getById(anyLong())).thenReturn(null);

        // Act & Assert
        mockMvc.perform(post("/product/999/delete")
                        .with(csrf())
                        .with(user(testUser)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        verify(productService, never()).deleteProductById(anyLong());
    }
}