package com.example.services;

import com.example.models.Product;
import com.example.models.User;
import com.example.models.enums.Role;
import com.example.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProductImageService productImageService;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public boolean registerUser(User user) {
        if(userRepository.existsByUsername(user.getUsername())) {
            log.error("Username already exists: {}", user.getUsername());
            return false;
        }

        if (userRepository.existsByEmail(user.getEmail())) {
            log.error("Email already exists: {}", user.getEmail());
            return false;
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.addRole(Role.ROLE_USER);

        userRepository.save(user);
        log.info("User registered successfully: {}", user.getUsername());
        return true;
    }

    public User getByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    @Transactional
    public boolean updateUser(Long id, String username, String email, String newPassword) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            log.error("User with id {} not found", id);
            return false;
        }

        // If usernames aren't equals AND new username already exists
        if (!user.getUsername().equals(username) && userRepository.existsByUsername(username)) {
            log.error("Username already exists: {}", username);
            return false;
        }

        // If email aren't equals AND new email already exists
        if (!user.getEmail().equals(email) && userRepository.existsByEmail(email)) {
            log.error("Email already exists: {}", email);
            return false;
        }

        user.setUsername(username);
        user.setEmail(email);

        if (newPassword != null && !newPassword.isBlank()) {
            user.setPassword(passwordEncoder.encode(newPassword));
        }

        userRepository.save(user);
        log.info("User updated successfully: {}", user.getUsername());
        return true;
    }

    public User getById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            log.error("User with id {} not found", id);
            return;
        }

        if (user.getProducts() != null && !user.getProducts().isEmpty()) {
            for (Product product : user.getProducts()) {
                if (product.getImages() != null) {
                    for (var image : product.getImages()) {
                        productImageService.deleteImageFromDisk(image);
                    }
                }
            }
            log.info("Deleted {} products for user: {}", user.getProducts().size(), user.getUsername());
        }

        userRepository.deleteById(id);
        log.info("User deleted: {}", user.getUsername());
    }


    // ========================================
    // Admin methods
    // ========================================

    @Transactional
    public boolean toggleUserActive(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.error("User with id {} not found", userId);
            return false;
        }

        user.setActive(!user.isActive());
        userRepository.save(user);
        log.info("User {} active status changed to: {}", user.getUsername(), user.isActive());
        return true;
    }

    @Transactional
    public boolean addRoleToUser(Long userId, Role role) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.error("User with id {} not found", userId);
            return false;
        }

        user.addRole(role);
        userRepository.save(user);
        log.info("Added role {} to user: {}", role, user.getUsername());
        return true;
    }

    @Transactional
    public boolean removeRoleFromUser(Long userId, Role role) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.error("User with id {} not found", userId);
            return false;
        }

        user.removeRole(role);
        userRepository.save(user);
        log.info("Removed role {} from user: {}", role, user.getUsername());
        return true;
    }

    public List<User> getByUsernameOrEmail(String query) {
        return userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(query, query);
    }
}