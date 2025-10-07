package com.example.controllers;

import com.example.models.Product;
import com.example.services.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
                             Model model) throws IOException {

        boolean isPreviewImageMissing = false;
        if(previewImage == null || previewImage.isEmpty()) {
            model.addAttribute("errorPreviewImageMissing", "Preview image is required");
            isPreviewImageMissing = true;
        }

        if(bindingResult.hasErrors() ||  isPreviewImageMissing) {
            return "product-add";
        }

        if(!productService.saveProductWithImages(previewImage, additionalImages, product)) {
            return "product-add";
        }

        return "redirect:/";
    }
}
