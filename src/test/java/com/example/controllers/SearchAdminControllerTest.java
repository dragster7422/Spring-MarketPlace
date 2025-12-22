package com.example.controllers;

import com.example.configurations.SecurityConfig;
import com.example.models.User;
import com.example.models.enums.Role;
import com.example.services.ProductSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SearchAdminController.class)
@Import(SecurityConfig.class)
class SearchAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductSearchService searchService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    private User adminUser;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@example.com");
        adminUser.getRoles().add(Role.ROLE_ADMIN);
        adminUser.getRoles().add(Role.ROLE_USER);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void reindexAllProducts_ShouldReindexAndRedirect() throws Exception {
        // Arrange
        doNothing().when(searchService).reindexAllProducts();

        // Act & Assert
        mockMvc.perform(post("/profile/admin/search/reindex")
                        .with(csrf())
                        .with(user(adminUser)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile/admin/dashboard?reindexed=true"));

        verify(searchService, times(1)).reindexAllProducts();
    }

    @Test
    @WithMockUser(roles = "USER")
    void reindexAllProducts_AsRegularUser_ShouldBeForbidden() throws Exception {
        // Arrange
        User regularUser = new User();
        regularUser.setId(2L);
        regularUser.setUsername("user");
        regularUser.getRoles().add(Role.ROLE_USER);

        // Act & Assert
        mockMvc.perform(post("/profile/admin/search/reindex")
                        .with(csrf())
                        .with(user(regularUser)))
                .andExpect(status().isForbidden());

        verify(searchService, never()).reindexAllProducts();
    }

    @Test
    void reindexAllProducts_WithoutAuthentication_ShouldBeUnauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/profile/admin/search/reindex")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        verify(searchService, never()).reindexAllProducts();
    }
}