package com.example.services;

import com.example.elasticsearch.ProductDocument;
import com.example.models.Product;
import com.example.repositories.ProductRepository;
import com.example.repositories.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSearchService {

    private final ProductSearchRepository searchRepository;
    private final ProductRepository productRepository;

    /**
     * Index a product in Elasticsearch
     */
    public void indexProduct(Product product) {
        try {
            ProductDocument document = new ProductDocument(product);
            searchRepository.save(document);
            log.info("Product indexed successfully: {}", product.getId());
        } catch (Exception e) {
            log.error("Error indexing product: {}", product.getId(), e);
        }
    }

    /**
     * Delete product from Elasticsearch index
     */
    public void deleteProductFromIndex(Long productId) {
        try {
            searchRepository.deleteById(productId);
            log.info("Product deleted from index: {}", productId);
        } catch (Exception e) {
            log.error("Error deleting product from index: {}", productId, e);
        }
    }

    /**
     * Reindex all products
     */
    public void reindexAllProducts() {
        try {
            log.info("Starting reindexing of all products...");
            searchRepository.deleteAll();

            List<Product> allProducts = productRepository.findAll();
            List<ProductDocument> documents = allProducts.stream()
                    .map(ProductDocument::new)
                    .collect(Collectors.toList());

            searchRepository.saveAll(documents);
            log.info("Reindexing completed. Total products: {}", documents.size());
        } catch (Exception e) {
            log.error("Error during reindexing", e);
        }
    }

    /**
     * Search products by query (searches in title and description)
     */
    public Page<Product> searchProducts(String query, int page, int size) {
        if (query == null || query.trim().isEmpty()) {
            Pageable pageable = PageRequest.of(page, size, Sort.by("dateOfCreated").descending());
            return productRepository.findAll(pageable);
        }

        try {
            Pageable pageable = PageRequest.of(page, size);

            // Search in Elasticsearch
            Page<ProductDocument> documentsPage = searchRepository
                    .findByTitleContainingOrDescriptionContaining(query, query, pageable);

            // Get full Product entities from DB
            List<Long> productIds = documentsPage.getContent().stream()
                    .map(ProductDocument::getId)
                    .collect(Collectors.toList());

            List<Product> products = productRepository.findAllById(productIds);

            // Return as Page
            return new PageImpl<>(products, pageable, documentsPage.getTotalElements());
        } catch (Exception e) {
            log.error("Error searching products with query: {}", query, e);
            // Fallback to database search if Elasticsearch fails
            Pageable pageable = PageRequest.of(page, size, Sort.by("dateOfCreated").descending());
            return productRepository.findAll(pageable);
        }
    }
}