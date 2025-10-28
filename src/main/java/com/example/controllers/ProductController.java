package com.example.controllers;

import com.example.models.Product;
import com.example.services.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    @GetMapping("/")
    public String products(Model model) {
        model.addAttribute("products", productService.getProducts());
        return "products";
    }

    /*	Add */
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
                             Model model) {

        boolean isPreviewImageMissing = false;
        if(previewImage == null || previewImage.isEmpty()) {
            model.addAttribute("errorPreviewImageMissing", "Preview image is required");
            isPreviewImageMissing = true;
        }

        if(bindingResult.hasErrors() ||  isPreviewImageMissing) {
            return "product-add";
        }

        try {
            if(!productService.saveProductWithImages(previewImage, additionalImages, product)) {
                model.addAttribute("errorSaving", "Failed to save product. Please try again.");
                return "product-add";
            }
        } catch (IOException e) {
            model.addAttribute("errorSaving", "Error uploading images: " + e.getMessage());
            return "product-add";
        }

        return "redirect:/";
    }


    // Details
    @GetMapping("/product/{id}")
    public String productDetails(@PathVariable Long id, Model model) {
        Product product = productService.getById(id);
        if(product == null) {
            return "redirect:/";
        }
        model.addAttribute("product", product);
        return "product-details";
    }

    // Edit
    @GetMapping("/product/{id}/edit")
    public String productEdit(@PathVariable Long id, Model model) {
        Product product = productService.getById(id);
        if(product == null) {
            return "redirect:/";
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
                              Model model) {

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
    public String productDelete(@PathVariable Long id) {
        productService.deleteProductById(id);
        return "redirect:/";
    }
}
