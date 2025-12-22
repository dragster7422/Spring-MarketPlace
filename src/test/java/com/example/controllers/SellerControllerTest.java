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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SellerController.class)
@Import(SecurityConfig.class)
class SellerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private ProductService productService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    private User sellerUser;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        sellerUser = new User();
        sellerUser.setId(1L);
        sellerUser.setUsername("seller");
        sellerUser.setEmail("seller@example.com");
        sellerUser.getRoles().add(Role.ROLE_USER);

        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setTitle("Test Product");
        testProduct.setDescription("Test Description");
        testProduct.setPrice(new BigDecimal("99.99"));
        testProduct.setOwner(sellerUser);

        // Add preview image
        ProductImage previewImage = new ProductImage();
        previewImage.setId(1L);
        previewImage.setImageDirectory("uploads/test-image.jpg");
        previewImage.setPreviewImage(true);
        testProduct.setImages(List.of(previewImage));
    }

    @Test
    void sellerProfile_ShouldReturnSellerProfilePage() throws Exception {
        // Arrange
        List<Product> products = List.of(testProduct);
        when(userService.getById(anyLong())).thenReturn(sellerUser);
        when(productService.getProductsByOwnerId(anyLong())).thenReturn(products);

        // Act & Assert
        mockMvc.perform(get("/seller/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile-seller"))
                .andExpect(model().attributeExists("seller"))
                .andExpect(model().attributeExists("products"))
                .andExpect(model().attribute("seller", sellerUser))
                .andExpect(model().attribute("products", products));

        verify(userService, times(1)).getById(1L);
        verify(productService, times(1)).getProductsByOwnerId(1L);
    }

    @Test
    void sellerProfile_WhenSellerNotFound_ShouldRedirect() throws Exception {
        // Arrange
        when(userService.getById(anyLong())).thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/seller/999"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        verify(userService, times(1)).getById(999L);
        verify(productService, never()).getProductsByOwnerId(anyLong());
    }

    @Test
    void sellerProfile_WithNoProducts_ShouldReturnEmptyList() throws Exception {
        // Arrange
        when(userService.getById(anyLong())).thenReturn(sellerUser);
        when(productService.getProductsByOwnerId(anyLong())).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/seller/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile-seller"))
                .andExpect(model().attribute("products", List.of()));

        verify(productService, times(1)).getProductsByOwnerId(1L);
    }

    @Test
    void sellerProfile_WithMultipleProducts_ShouldReturnAllProducts() throws Exception {
        // Arrange
        Product product2 = new Product();
        product2.setId(2L);
        product2.setTitle("Another Product");
        product2.setDescription("Another Description");
        product2.setPrice(new BigDecimal("149.99"));
        product2.setOwner(sellerUser);

        // Add preview image to product2
        ProductImage previewImage2 = new ProductImage();
        previewImage2.setId(2L);
        previewImage2.setImageDirectory("uploads/test-image-2.jpg");
        previewImage2.setPreviewImage(true);
        product2.setImages(List.of(previewImage2));

        List<Product> products = List.of(testProduct, product2);
        when(userService.getById(anyLong())).thenReturn(sellerUser);
        when(productService.getProductsByOwnerId(anyLong())).thenReturn(products);

        // Act & Assert
        mockMvc.perform(get("/seller/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile-seller"))
                .andExpect(model().attribute("products", products));

        verify(productService, times(1)).getProductsByOwnerId(1L);
    }
}