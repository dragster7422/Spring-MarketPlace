package com.example.controllers;

import com.example.configurations.SecurityConfig;
import com.example.models.User;
import com.example.models.enums.Role;
import com.example.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    private User adminUser;
    private User regularUser;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@example.com");
        adminUser.getRoles().add(Role.ROLE_ADMIN);
        adminUser.getRoles().add(Role.ROLE_USER);

        regularUser = new User();
        regularUser.setId(2L);
        regularUser.setUsername("user");
        regularUser.setEmail("user@example.com");
        regularUser.getRoles().add(Role.ROLE_USER);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminDashboard_ShouldReturnDashboardPage() throws Exception {
        // Arrange
        List<User> users = List.of(regularUser);
        Page<User> userPage = new PageImpl<>(users);
        when(userService.getUsersPage(anyInt(), anyInt())).thenReturn(userPage);

        // Act & Assert
        mockMvc.perform(get("/profile/admin/dashboard").with(user(adminUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-dashboard"))
                .andExpect(model().attributeExists("users"))
                .andExpect(model().attributeExists("currentPage"))
                .andExpect(model().attributeExists("totalPages"));

        verify(userService, times(1)).getUsersPage(0, 20);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminDashboard_WithSearchQuery_ShouldReturnFilteredResults() throws Exception {
        // Arrange
        List<User> users = List.of(regularUser);
        Page<User> userPage = new PageImpl<>(users);
        when(userService.getByUsernameOrEmailPage(anyString(), anyInt(), anyInt())).thenReturn(userPage);

        // Act & Assert
        mockMvc.perform(get("/profile/admin/dashboard")
                        .with(user(adminUser))
                        .param("query", "user"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-dashboard"))
                .andExpect(model().attribute("searchQuery", "user"));

        verify(userService, times(1)).getByUsernameOrEmailPage("user", 0, 20);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void toggleBan_ShouldBanUserAndRedirect() throws Exception {
        // Arrange
        when(userService.toggleUserActive(anyLong())).thenReturn(true);

        // Act & Assert
        mockMvc.perform(post("/profile/admin/dashboard/user/2/toggle-ban")
                        .with(csrf())
                        .with(user(adminUser)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile/admin/dashboard"));

        verify(userService, times(1)).toggleUserActive(2L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void toggleBan_OnSelf_ShouldNotBanAndReturnError() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/profile/admin/dashboard/user/1/toggle-ban")
                        .with(csrf())
                        .with(user(adminUser)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile/admin/dashboard?error=cannotBanSelf"));

        verify(userService, never()).toggleUserActive(anyLong());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void addRole_ShouldAddRoleAndRedirect() throws Exception {
        // Arrange
        when(userService.addRoleToUser(anyLong(), any(Role.class))).thenReturn(true);

        // Act & Assert
        mockMvc.perform(post("/profile/admin/dashboard/user/2/add-role")
                        .with(csrf())
                        .with(user(adminUser))
                        .param("role", "ROLE_ADMIN"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile/admin/dashboard"));

        verify(userService, times(1)).addRoleToUser(2L, Role.ROLE_ADMIN);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void removeRole_ShouldRemoveRoleAndRedirect() throws Exception {
        // Arrange
        Set<Role> roles = new HashSet<>();
        roles.add(Role.ROLE_USER);
        roles.add(Role.ROLE_ADMIN);
        regularUser.setRoles(roles);

        when(userService.getById(anyLong())).thenReturn(regularUser);
        when(userService.removeRoleFromUser(anyLong(), any(Role.class))).thenReturn(true);

        // Act & Assert
        mockMvc.perform(post("/profile/admin/dashboard/user/2/remove-role")
                        .with(csrf())
                        .with(user(adminUser))
                        .param("role", "ROLE_ADMIN"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile/admin/dashboard"));

        verify(userService, times(1)).removeRoleFromUser(2L, Role.ROLE_ADMIN);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void removeRole_LastRole_ShouldReturnError() throws Exception {
        // Arrange
        when(userService.getById(anyLong())).thenReturn(regularUser);

        // Act & Assert
        mockMvc.perform(post("/profile/admin/dashboard/user/2/remove-role")
                        .with(csrf())
                        .with(user(adminUser))
                        .param("role", "ROLE_USER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile/admin/dashboard?error=cannotRemoveLastRole"));

        verify(userService, never()).removeRoleFromUser(anyLong(), any(Role.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void removeRole_OwnAdminRole_ShouldReturnError() throws Exception {
        // Arrange
        when(userService.getById(anyLong())).thenReturn(adminUser);

        // Act & Assert
        mockMvc.perform(post("/profile/admin/dashboard/user/1/remove-role")
                        .with(csrf())
                        .with(user(adminUser))
                        .param("role", "ROLE_ADMIN"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile/admin/dashboard?error=cannotRemoveOwnAdminRole"));

        verify(userService, never()).removeRoleFromUser(anyLong(), any(Role.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteUser_ShouldDeleteAndRedirect() throws Exception {
        // Arrange
        doNothing().when(userService).deleteUser(anyLong());

        // Act & Assert
        mockMvc.perform(post("/profile/admin/dashboard/user/2/delete")
                        .with(csrf())
                        .with(user(adminUser)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile/admin/dashboard"));

        verify(userService, times(1)).deleteUser(2L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteUser_OnSelf_ShouldReturnError() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/profile/admin/dashboard/user/1/delete")
                        .with(csrf())
                        .with(user(adminUser)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile/admin/dashboard?error=cannotDeleteSelf"));

        verify(userService, never()).deleteUser(anyLong());
    }

    @Test
    @WithMockUser(roles = "USER")
    void adminDashboard_AsRegularUser_ShouldBeForbidden() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/profile/admin/dashboard").with(user(regularUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminDashboard_WithPagination_ShouldReturnCorrectPage() throws Exception {
        // Arrange
        List<User> users = List.of(regularUser);
        Page<User> userPage = new PageImpl<>(users);
        when(userService.getUsersPage(anyInt(), anyInt())).thenReturn(userPage);

        // Act & Assert
        mockMvc.perform(get("/profile/admin/dashboard")
                        .with(user(adminUser))
                        .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-dashboard"));

        verify(userService, times(1)).getUsersPage(1, 20);
    }
}