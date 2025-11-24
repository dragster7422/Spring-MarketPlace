package com.example.controllers;

import com.example.models.Product;
import com.example.models.User;
import com.example.services.ProductService;
import com.example.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class SellerController {
    private final UserService userService;
    private final ProductService productService;

    @GetMapping("/seller/{id}")
    public String sellerProfile(@PathVariable Long id, Model model) {
        User seller = userService.getById(id);

        if (seller == null) {
            return "redirect:/";
        }

        List<Product> products = productService.getProductsByOwnerId(id);

        model.addAttribute("seller", seller);
        model.addAttribute("products", products);

        return "profile-seller";
    }
}