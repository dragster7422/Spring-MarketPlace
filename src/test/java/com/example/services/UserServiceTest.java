package com.example.services;

import com.example.models.Product;
import com.example.models.ProductImage;
import com.example.models.User;
import com.example.models.enums.Role;
import com.example.repositories.UserRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ProductImageService productImageService;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password123");
        testUser.setActive(true);
        testUser.getRoles().add(Role.ROLE_USER);
    }

    @Test
    void getAllUsers_ShouldReturnListOfUsers() {
        // Arrange
        List<User> users = List.of(testUser);
        when(userRepository.findAll()).thenReturn(users);

        // Act
        List<User> result = userService.getAllUsers();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testUser.getUsername(), result.get(0).getUsername());
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void getUsersPage_ShouldReturnPageOfUsers() {
        // Arrange
        List<User> users = List.of(testUser);
        Page<User> userPage = new PageImpl<>(users);
        when(userRepository.findAll(any(Pageable.class))).thenReturn(userPage);

        // Act
        Page<User> result = userService.getUsersPage(0, 20);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(testUser.getUsername(), result.getContent().get(0).getUsername());
        verify(userRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    void registerUser_WithValidData_ShouldReturnTrue() {
        // Arrange
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        boolean result = userService.registerUser(testUser);

        // Assert
        assertTrue(result);
        verify(userRepository, times(1)).existsByUsername(testUser.getUsername());
        verify(userRepository, times(1)).existsByEmail(testUser.getEmail());
        verify(passwordEncoder, times(1)).encode("password123");
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void registerUser_WithExistingUsername_ShouldReturnFalse() {
        // Arrange
        when(userRepository.existsByUsername(anyString())).thenReturn(true);

        // Act
        boolean result = userService.registerUser(testUser);

        // Assert
        assertFalse(result);
        verify(userRepository, times(1)).existsByUsername(testUser.getUsername());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUser_WithExistingEmail_ShouldReturnFalse() {
        // Arrange
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        // Act
        boolean result = userService.registerUser(testUser);

        // Assert
        assertFalse(result);
        verify(userRepository, times(1)).existsByEmail(testUser.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getByUsername_WhenUserExists_ShouldReturnUser() {
        // Arrange
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(testUser));

        // Act
        User result = userService.getByUsername("testuser");

        // Assert
        assertNotNull(result);
        assertEquals(testUser.getUsername(), result.getUsername());
        verify(userRepository, times(1)).findByUsername("testuser");
    }

    @Test
    void getByUsername_WhenUserDoesNotExist_ShouldReturnNull() {
        // Arrange
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        // Act
        User result = userService.getByUsername("nonexistent");

        // Assert
        assertNull(result);
        verify(userRepository, times(1)).findByUsername("nonexistent");
    }

    @Test
    void updateUser_WithValidData_ShouldReturnTrue() {
        // Arrange
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        boolean result = userService.updateUser(1L, "newusername", "newemail@example.com", null);

        // Assert
        assertTrue(result);
        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void updateUser_WithPassword_ShouldEncodePassword() {
        // Arrange
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(anyString())).thenReturn("newEncodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        boolean result = userService.updateUser(1L, "testuser", "test@example.com", "newPassword");

        // Assert
        assertTrue(result);
        verify(passwordEncoder, times(1)).encode("newPassword");
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void updateUser_WhenUserNotFound_ShouldReturnFalse() {
        // Arrange
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act
        boolean result = userService.updateUser(1L, "newusername", "newemail@example.com", null);

        // Assert
        assertFalse(result);
        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getById_WhenUserExists_ShouldReturnUser() {
        // Arrange
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));

        // Act
        User result = userService.getById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(testUser.getId(), result.getId());
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    void getById_WhenUserDoesNotExist_ShouldReturnNull() {
        // Arrange
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act
        User result = userService.getById(999L);

        // Assert
        assertNull(result);
        verify(userRepository, times(1)).findById(999L);
    }

    @Test
    void deleteUser_WithProducts_ShouldDeleteImagesAndUser() {
        // Arrange
        Product product = new Product();
        ProductImage image = new ProductImage();
        product.setImages(new ArrayList<>(List.of(image)));
        testUser.setProducts(new ArrayList<>(List.of(product)));

        when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
        doNothing().when(productImageService).deleteImageFromDisk(any(ProductImage.class));
        doNothing().when(userRepository).deleteById(anyLong());

        // Act
        userService.deleteUser(1L);

        // Assert
        verify(userRepository, times(1)).findById(1L);
        verify(productImageService, times(1)).deleteImageFromDisk(image);
        verify(userRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteUser_WhenUserNotFound_ShouldNotDelete() {
        // Arrange
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act
        userService.deleteUser(999L);

        // Assert
        verify(userRepository, times(1)).findById(999L);
        verify(userRepository, never()).deleteById(anyLong());
    }

    @Test
    void toggleUserActive_ShouldChangeActiveStatus() {
        // Arrange
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        boolean result = userService.toggleUserActive(1L);

        // Assert
        assertTrue(result);
        assertFalse(testUser.isActive());
        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void addRoleToUser_ShouldAddRole() {
        // Arrange
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        boolean result = userService.addRoleToUser(1L, Role.ROLE_ADMIN);

        // Assert
        assertTrue(result);
        assertTrue(testUser.hasRole(Role.ROLE_ADMIN));
        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void removeRoleFromUser_ShouldRemoveRole() {
        // Arrange
        testUser.addRole(Role.ROLE_ADMIN);
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        boolean result = userService.removeRoleFromUser(1L, Role.ROLE_ADMIN);

        // Assert
        assertTrue(result);
        assertFalse(testUser.hasRole(Role.ROLE_ADMIN));
        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void getByUsernameOrEmailPage_WithQuery_ShouldReturnFilteredResults() {
        // Arrange
        List<User> users = List.of(testUser);
        Page<User> userPage = new PageImpl<>(users);
        when(userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                anyString(), anyString(), any(Pageable.class))).thenReturn(userPage);

        // Act
        Page<User> result = userService.getByUsernameOrEmailPage("test", 0, 20);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(userRepository, times(1))
                .findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                        eq("test"), eq("test"), any(Pageable.class));
    }

    @Test
    void getByUsernameOrEmailPage_WithEmptyQuery_ShouldReturnAllUsers() {
        // Arrange
        List<User> users = List.of(testUser);
        Page<User> userPage = new PageImpl<>(users);
        when(userRepository.findAll(any(Pageable.class))).thenReturn(userPage);

        // Act
        Page<User> result = userService.getByUsernameOrEmailPage("", 0, 20);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(userRepository, times(1)).findAll(any(Pageable.class));
    }
}