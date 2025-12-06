package com.example.controllers;

import com.example.models.User;
import com.example.models.enums.Role;
import com.example.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/profile/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {
    private final UserService userService;

    @GetMapping("")
    public String adminDashboard(@AuthenticationPrincipal User currentAdmin,
                                 @RequestParam(name = "query", required = false) String query,
                                 Model model) {

        List<User> users;
        if (query != null && !query.trim().isEmpty()) {
            users = userService.getByUsernameOrEmail(query);
            model.addAttribute("searchQuery", query);
        } else {
            users = userService.getAllUsers();
        }

        model.addAttribute("users", users);
        model.addAttribute("currentAdmin", currentAdmin);
        return "admin-dashboard";
    }

    @PostMapping("/user/{id}/toggle-ban")
    public String toggleBan(@PathVariable Long id,
                            @AuthenticationPrincipal User currentAdmin) {
        // Prevent admin from banning themselves
        if (id.equals(currentAdmin.getId())) {
            return "redirect:/profile/admin/dashboard?error=cannotBanSelf";
        }

        userService.toggleUserActive(id);
        return "redirect:/profile/admin/dashboard";
    }

    @PostMapping("/user/{id}/add-role")
    public String addRole(@PathVariable Long id,
                          @RequestParam("role") Role role) {
        userService.addRoleToUser(id, role);
        return "redirect:/profile/admin/dashboard";
    }

    @PostMapping("/user/{id}/remove-role")
    public String removeRole(@PathVariable Long id,
                             @RequestParam("role") Role role,
                             @AuthenticationPrincipal User currentAdmin) {
        // Prevent removing all roles
        User user = userService.getById(id);
        if (user != null && user.getRoles().size() <= 1) {
            return "redirect:/profile/admin/dashboard?error=cannotRemoveLastRole";
        }

        // Prevent admin from removing their own ADMIN role
        if (id.equals(currentAdmin.getId()) && role == Role.ROLE_ADMIN) {
            return "redirect:/profile/admin/dashboard?error=cannotRemoveOwnAdminRole";
        }

        userService.removeRoleFromUser(id, role);
        return "redirect:/profile/admin/dashboard";
    }

    @PostMapping("/user/{id}/delete")
    public String deleteUser(@PathVariable Long id,
                             @AuthenticationPrincipal User currentAdmin) {
        // Prevent admin from deleting themselves
        if (id.equals(currentAdmin.getId())) {
            return "redirect:/profile/admin/dashboard?error=cannotDeleteSelf";
        }

        userService.deleteUser(id);
        return "redirect:/profile/admin/dashboard";
    }
}