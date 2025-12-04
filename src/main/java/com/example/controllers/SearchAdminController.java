package com.example.controllers;

import com.example.services.ProductSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/profile/admin/search")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class SearchAdminController {

    private final ProductSearchService searchService;

    /**
     * Reindex all products in Elasticsearch
     * Accessible only for admins
     */
    @PostMapping("/reindex")
    public String reindexAllProducts() {
        searchService.reindexAllProducts();
        return "redirect:/profile/admin/dashboard?reindexed=true";
    }
}