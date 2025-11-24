package com.example.controllers;

import com.example.dto.UpdateProfileDto;
import com.example.models.Product;
import com.example.models.User;
import com.example.services.ProductService;
import com.example.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final ProductService productService;

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.getByUsername(userDetails.getUsername());
        model.addAttribute("user", user);
        return "profile";
    }

    @GetMapping("/profile/edit")
    public String editProfile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.getByUsername(userDetails.getUsername());

        UpdateProfileDto updateDto = new UpdateProfileDto();
        updateDto.setUsername(user.getUsername());
        updateDto.setEmail(user.getEmail());

        model.addAttribute("updateProfileDto", updateDto);
        model.addAttribute("user", user);
        return "profile-edit";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@AuthenticationPrincipal UserDetails userDetails,
                                @Valid @ModelAttribute UpdateProfileDto updateDto,
                                BindingResult bindingResult,
                                Model model,
                                HttpServletRequest request) {

        User currentUser = userService.getByUsername(userDetails.getUsername());

        // For example:
        // 1. The admin deleted the user, but the session is still active.
        // 2. The user changed the username in another tab, but the old username remains in the current session.
        if (currentUser == null) {
            return "redirect:/logout";
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("user", currentUser);
            return "profile-edit";
        }

        // Checking for password errors
        if (updateDto.getPassword() != null && !updateDto.getPassword().isBlank() && updateDto.getPassword().length() < 6) {
            model.addAttribute("errorUpdate", "Password must be at least 6 characters");
            model.addAttribute("user", currentUser);
            return "profile-edit";
        }

        // Update data user
        if (!userService.updateUser(currentUser.getId(), updateDto.getUsername(), updateDto.getEmail(), updateDto.getPassword())) {
            model.addAttribute("errorUpdate", "Username or email already exists");
            model.addAttribute("user", currentUser);
            return "profile-edit";
        }

        // Update user session if username changed
        if (!updateDto.getUsername().equals(userDetails.getUsername())) {
            User updatedUser = userService.getByUsername(updateDto.getUsername());
            UserDetails newUserDetails = new org.springframework.security.core.userdetails.User(
                    updatedUser.getUsername(),
                    updatedUser.getPassword(),
                    new ArrayList<>()
            );

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    newUserDetails,
                    newUserDetails.getPassword(),
                    newUserDetails.getAuthorities()
            );

            // Replaces the old authentication with the new one
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Get session if it exists. FALSE means do not create a new session.
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
            }
        }

        // Success
        model.addAttribute("successUpdate", "Profile updated successfully!");
        model.addAttribute("user", userService.getByUsername(updateDto.getUsername()));
        return "profile-edit";
    }

    @PostMapping("/profile/delete")
    public String deleteProfile(@AuthenticationPrincipal UserDetails userDetails,
                                HttpServletRequest request) {
        User currentUser = userService.getByUsername(userDetails.getUsername());

        if(currentUser == null) {
            return "redirect:/logout";
        }

        userService.deleteUser(currentUser.getId());

        // Deletes user information in object Authentication
        SecurityContextHolder.clearContext();

        // Get session if it exists. FALSE means do not create a new session.
        HttpSession session = request.getSession(false);
        if(session != null) {
            // Removes all information from the session.
            session.invalidate();
        }

        return "redirect:/register?accountDeleted";
    }

    @GetMapping("/profile/products")
    public String myProducts(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User currentUser = userService.getByUsername(userDetails.getUsername());

        if(currentUser == null) {
            return "redirect:/logout";
        }

        List<Product> userProducts = productService.getProductsByOwnerId(currentUser.getId());

        model.addAttribute("products", userProducts);
        model.addAttribute("user", currentUser);

        return "profile-products";
    }
}
