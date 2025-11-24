package com.example.controllers;

import com.example.models.Product;
import com.example.models.User;
import com.example.services.ProductService;
import com.example.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;
    private final UserService userService;

    @GetMapping("/")
    public String products(Model model) {
        model.addAttribute("products", productService.getProducts());
        return "products";
    }


    // Add
    @GetMapping("/product/add")
    public String addProduct(Model model) {
        model.addAttribute("product", new Product());
        return "product-add";
    }

    @PostMapping("/product/add")
    public String addProduct(@RequestParam("previewImage") MultipartFile previewImage,
                             @RequestParam(value = "additionalImages", required = false) List<MultipartFile> additionalImages,
                             @Valid @ModelAttribute Product product,
                             BindingResult bindingResult,
                             @AuthenticationPrincipal UserDetails userDetails,
                             Model model) {

        boolean isPreviewImageMissing = false;
        if(previewImage == null || previewImage.isEmpty()) {
            model.addAttribute("errorPreviewImageMissing", "Preview image is required");
            isPreviewImageMissing = true;
        }

        if(bindingResult.hasErrors() || isPreviewImageMissing) {
            return "product-add";
        }

        // Get current User
        User currentUser = userService.getByUsername(userDetails.getUsername());
        if(currentUser == null) {
            return "redirect:/logout";
        }

        try {
            if(!productService.saveProductWithImages(previewImage, additionalImages, product, currentUser)) {
                model.addAttribute("errorSaving", "Failed to save product. Please try again.");
                return "product-add";
            }
        } catch (IOException e) {
            model.addAttribute("errorSaving", "Error uploading images: " + e.getMessage());
            return "product-add";
        }

        return "redirect:/profile/products";
    }


    // Details
    @GetMapping("/product/{id}")
    public String productDetails(@PathVariable Long id,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 Model model) {
        Product product = productService.getById(id);
        if(product == null) {
            return "redirect:/";
        }

        // Check if the current user is the owner
        boolean isOwner = false;
        if(userDetails != null) {
            User currentUser = userService.getByUsername(userDetails.getUsername());
            isOwner = productService.isOwner(product, currentUser);
        }

        model.addAttribute("product", product);
        model.addAttribute("isOwner", isOwner);
        return "product-details";
    }

    // Edit
    @GetMapping("/product/{id}/edit")
    public String productEdit(@PathVariable Long id,
                              @AuthenticationPrincipal UserDetails userDetails,
                              Model model) {
        Product product = productService.getById(id);
        if(product == null) {
            return "redirect:/";
        }

        // Checking access rights
        User currentUser = userService.getByUsername(userDetails.getUsername());
        if(!productService.isOwner(product, currentUser)) {
            model.addAttribute("errorAccess", "You don't have permission to edit this product");
            return "redirect:/product/" + id;
        }

        model.addAttribute("product", product);
        return "product-edit";
    }

    @PostMapping("product/{id}/edit")
    public String productEdit(@PathVariable Long id,
                              @RequestParam(value = "previewImage", required = false) MultipartFile previewImage,
                              @RequestParam(value = "additionalImages", required = false) List<MultipartFile> additionalImages,
                              @RequestParam(value = "removeImageIds", required = false) List<Long> removeImageIds,
                              @Valid @ModelAttribute Product product,
                              BindingResult bindingResult,
                              @AuthenticationPrincipal UserDetails userDetails,
                              Model model) {

        // Checking access rights
        Product dbProduct = productService.getById(id);
        if(dbProduct == null) {
            return "redirect:/";
        }

        User currentUser = userService.getByUsername(userDetails.getUsername());
        if(!productService.isOwner(dbProduct, currentUser)) {
            return "redirect:/product/" + id;
        }

        if(bindingResult.hasErrors()) {
            return "product-edit";
        }

        if(!productService.updateProduct(id, previewImage, additionalImages, removeImageIds, product)) {
            model.addAttribute("errorSaving", "Failed to update product. Please try again.");
            return "product-edit";
        }

        return "redirect:/product/" + id;
    }


    // Delete
    @PostMapping("/product/{id}/delete")
    public String productDelete(@PathVariable Long id,
                                @AuthenticationPrincipal UserDetails userDetails) {

        // Checking access rights
        Product product = productService.getById(id);
        if(product == null) {
            return "redirect:/";
        }

        User currentUser = userService.getByUsername(userDetails.getUsername());
        if(!productService.isOwner(product, currentUser)) {
            return "redirect:/product/" + id;
        }

        productService.deleteProductById(id);
        return "redirect:/profile/products";
    }
}
