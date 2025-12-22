package com.example.controllers;

import com.example.configurations.SecurityConfig;
import com.example.dto.UpdateProfileDto;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private ProductService productService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.getRoles().add(Role.ROLE_USER);

        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setTitle("Test Product");
        testProduct.setDescription("Test Description");
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
    @WithMockUser
    void profile_ShouldReturnProfilePage() throws Exception {
        mockMvc.perform(get("/profile").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andExpect(model().attributeExists("user"));
    }

    @Test
    @WithMockUser
    void editProfile_GET_ShouldReturnEditPage() throws Exception {
        mockMvc.perform(get("/profile/edit").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("profile-edit"))
                .andExpect(model().attributeExists("updateProfileDto"))
                .andExpect(model().attributeExists("user"));
    }

    @Test
    @WithMockUser
    void updateProfile_WithValidData_ShouldUpdateAndRedirect() throws Exception {
        // Arrange
        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setUsername("newusername");
        updatedUser.setEmail("newemail@example.com");
        updatedUser.setPassword("encodedPassword");
        updatedUser.getRoles().add(Role.ROLE_USER);

        when(userService.getById(1L)).thenReturn(testUser);
        when(userService.updateUser(anyLong(), anyString(), anyString(), any())).thenReturn(true);
        when(userService.getByUsername("newusername")).thenReturn(updatedUser);

        // Act & Assert
        mockMvc.perform(post("/profile/update")
                        .with(csrf())
                        .with(user(testUser))
                        .param("username", "newusername")
                        .param("email", "newemail@example.com")
                        .param("password", "newpassword"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile-edit"))
                .andExpect(model().attributeExists("successUpdate"));

        verify(userService, times(1)).updateUser(1L, "newusername", "newemail@example.com", "newpassword");
    }

    @Test
    @WithMockUser
    void updateProfile_WithUsernameChange_ShouldUpdateSession() throws Exception {
        // Arrange
        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setUsername("newusername");
        updatedUser.setEmail("newemail@example.com");
        updatedUser.setPassword("encodedPassword");
        updatedUser.getRoles().add(Role.ROLE_USER);

        when(userService.getById(anyLong())).thenReturn(testUser);
        when(userService.updateUser(anyLong(), anyString(), anyString(), any())).thenReturn(true);
        when(userService.getByUsername(eq("newusername"))).thenReturn(updatedUser);

        // Act & Assert
        mockMvc.perform(post("/profile/update")
                        .with(csrf())
                        .with(user(testUser))
                        .param("username", "newusername")
                        .param("email", "newemail@example.com")
                        .param("password", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("profile-edit"));

        verify(userService, times(1)).getByUsername("newusername");
    }

    @Test
    @WithMockUser
    void updateProfile_WhenUserNotFound_ShouldRedirectToLogout() throws Exception {
        // Arrange
        when(userService.getById(anyLong())).thenReturn(null);

        // Act & Assert
        mockMvc.perform(post("/profile/update")
                        .with(csrf())
                        .with(user(testUser))
                        .param("username", "newusername")
                        .param("email", "newemail@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/logout"));

        verify(userService, never()).updateUser(anyLong(), anyString(), anyString(), any());
    }

    @Test
    @WithMockUser
    void updateProfile_WithValidationErrors_ShouldReturnErrors() throws Exception {
        // Arrange
        when(userService.getById(anyLong())).thenReturn(testUser);

        // Act & Assert
        mockMvc.perform(post("/profile/update")
                        .with(csrf())
                        .with(user(testUser))
                        .param("username", "ab")
                        .param("email", "invalid-email"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile-edit"))
                .andExpect(model().hasErrors());

        verify(userService, never()).updateUser(anyLong(), anyString(), anyString(), any());
    }

    @Test
    @WithMockUser
    void updateProfile_WithShortPassword_ShouldReturnError() throws Exception {
        // Arrange
        when(userService.getById(anyLong())).thenReturn(testUser);

        // Act & Assert
        mockMvc.perform(post("/profile/update")
                        .with(csrf())
                        .with(user(testUser))
                        .param("username", "testuser")
                        .param("email", "test@example.com")
                        .param("password", "short"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile-edit"))
                .andExpect(model().attributeExists("errorUpdate"));

        verify(userService, never()).updateUser(anyLong(), anyString(), anyString(), any());
    }

    @Test
    @WithMockUser
    void updateProfile_WhenUpdateFails_ShouldReturnError() throws Exception {
        // Arrange
        when(userService.getById(anyLong())).thenReturn(testUser);
        when(userService.updateUser(anyLong(), anyString(), anyString(), any())).thenReturn(false);

        // Act & Assert
        mockMvc.perform(post("/profile/update")
                        .with(csrf())
                        .with(user(testUser))
                        .param("username", "existinguser")
                        .param("email", "test@example.com")
                        .param("password", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("profile-edit"))
                .andExpect(model().attributeExists("errorUpdate"));
    }

    @Test
    @WithMockUser
    void deleteProfile_ShouldDeleteAndRedirect() throws Exception {
        // Arrange
        doNothing().when(userService).deleteUser(anyLong());

        // Act & Assert
        mockMvc.perform(post("/profile/delete")
                        .with(csrf())
                        .with(user(testUser)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register?accountDeleted"));

        verify(userService, times(1)).deleteUser(1L);
    }

    @Test
    @WithMockUser
    void myProducts_ShouldReturnMyProductsPage() throws Exception {
        // Arrange
        List<Product> products = List.of(testProduct);
        when(productService.getProductsByOwnerId(anyLong())).thenReturn(products);

        // Act & Assert
        mockMvc.perform(get("/profile/products").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("profile-products"))
                .andExpect(model().attributeExists("products"))
                .andExpect(model().attributeExists("user"));

        verify(productService, times(1)).getProductsByOwnerId(1L);
    }

    @Test
    @WithMockUser
    void myProducts_WithNoProducts_ShouldReturnEmptyList() throws Exception {
        // Arrange
        when(productService.getProductsByOwnerId(anyLong())).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/profile/products").with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("profile-products"))
                .andExpect(model().attribute("products", List.of()));

        verify(productService, times(1)).getProductsByOwnerId(1L);
    }
}