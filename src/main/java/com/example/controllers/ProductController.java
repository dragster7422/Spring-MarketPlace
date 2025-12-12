package com.example.controllers;

import com.example.models.Product;
import com.example.models.User;
import com.example.services.ProductService;
import com.example.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public String products(@RequestParam(value = "query", required = false) String query,
                           @RequestParam(value = "page", defaultValue = "0") int page,
                           Model model) {
        int pageSize = 20;
        Page<Product> productsPage;

        if (query != null && !query.trim().isEmpty()) {
            productsPage = productService.searchProducts(query, page, pageSize);
            model.addAttribute("searchQuery", query);
        } else {
            productsPage = productService.getProducts(page, pageSize);
        }

        model.addAttribute("products", productsPage.getContent());
        model.addAttribute("currentPage", productsPage.getNumber());
        model.addAttribute("totalPages", productsPage.getTotalPages());
        model.addAttribute("totalItems", productsPage.getTotalElements());

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
                             @AuthenticationPrincipal User currentUser,
                             Model model) {

        boolean isPreviewImageMissing = false;
        if(previewImage == null || previewImage.isEmpty()) {
            model.addAttribute("errorPreviewImageMissing", "Preview image is required");
            isPreviewImageMissing = true;
        }

        if(bindingResult.hasErrors() || isPreviewImageMissing) {
            return "product-add";
        }

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
                                 @AuthenticationPrincipal User currentUser,
                                 Model model) {
        Product product = productService.getById(id);
        if(product == null) {
            return "redirect:/";
        }

        // Check if the current user is the owner
        boolean isOwner = false;
        if(currentUser != null) {
            isOwner = productService.isOwner(product, currentUser);
        }

        model.addAttribute("product", product);
        model.addAttribute("isOwner", isOwner);
        return "product-details";
    }

    // Edit
    @GetMapping("/product/{id}/edit")
    public String productEdit(@PathVariable Long id,
                              @AuthenticationPrincipal User currentUser,
                              Model model) {
        Product product = productService.getById(id);
        if(product == null) {
            return "redirect:/";
        }

        // Checking access rights
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
                              @AuthenticationPrincipal User currentUser,
                              Model model) {

        // Checking access rights
        Product dbProduct = productService.getById(id);
        if(dbProduct == null) {
            return "redirect:/";
        }

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
                                @AuthenticationPrincipal User currentUser) {

        // Checking access rights
        Product product = productService.getById(id);
        if(product == null) {
            return "redirect:/";
        }

        if(!productService.isOwner(product, currentUser)) {
            return "redirect:/product/" + id;
        }

        productService.deleteProductById(id);
        return "redirect:/profile/products";
    }
}
