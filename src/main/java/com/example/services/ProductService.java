package com.example.services;

import com.example.models.Product;
import com.example.models.ProductImage;
import com.example.models.User;
import com.example.repositories.ProductRepository;
import com.example.services.ImageValidationService.ValidationResult;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductImageService productImageService;
    private final ProductSearchService searchService;
    private final ImageValidationService imageValidationService;

    public Page<Product> getProducts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("dateOfCreated").descending());
        return productRepository.findAll(pageable);
    }

    public Product getById(Long id) {
        return productRepository.findById(id).orElse(null);
    }

    public void save(Product product) {
        productRepository.save(product);
        searchService.indexProduct(product);
    }

    public List<Product> getProductsByOwnerId(Long ownerId) {
        return productRepository.findByOwnerId(ownerId);
    }

    public boolean isOwner(Product product, User user) {
        if (product == null || user == null) {
            return false;
        }
        return product.getOwner() != null && product.getOwner().getId().equals(user.getId());
    }

    /**
     * Save product with images and validation
     *
     * @return SaveResult with success status and error message if any
     */
    public SaveResult saveProductWithImages(MultipartFile previewImage,
                                            List<MultipartFile> additionalImages,
                                            Product product,
                                            User owner) {

        if (product == null || owner == null) {
            return SaveResult.error("Invalid product or owner data");
        }

        // Validate preview image
        ValidationResult previewValidation = imageValidationService.validatePreviewImage(previewImage);
        if (!previewValidation.isValid()) {
            return SaveResult.error(previewValidation.getErrorMessage());
        }

        // Validate additional images
        ValidationResult additionalValidation = imageValidationService.validateAdditionalImages(additionalImages);
        if (!additionalValidation.isValid()) {
            return SaveResult.error(additionalValidation.getErrorMessage());
        }

        try {
            String uploadDir = ProductImage.DIRECTORY_IMAGES;
            Files.createDirectories(Paths.get(uploadDir));

            List<ProductImage> images = new ArrayList<>();

            // Process preview image
            ProductImage preview = parseMultipartFileToUniqueFileName(uploadDir, previewImage, true);
            if (preview != null) {
                preview.setProduct(product);
                images.add(preview);
            } else {
                log.error("Failed to save preview image");
                return SaveResult.error("Failed to save preview image");
            }

            // Process additional images
            if (additionalImages != null && !additionalImages.isEmpty()) {
                for (MultipartFile file : additionalImages) {
                    if (file != null && !file.isEmpty()) {
                        ProductImage img = parseMultipartFileToUniqueFileName(uploadDir, file, false);
                        if (img != null) {
                            img.setProduct(product);
                            images.add(img);
                        } else {
                            log.error("Failed to save additional image");
                            // Clean up already saved images
                            cleanupImages(images);
                            return SaveResult.error("Failed to save additional image");
                        }
                    }
                }
            }

            product.setImages(images);
            product.setOwner(owner);
            productRepository.save(product);

            // Index product in Elasticsearch
            searchService.indexProduct(product);

            return SaveResult.success();

        } catch (IOException e) {
            log.error("IO Error while saving product images", e);
            return SaveResult.error("Error uploading images: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error while saving product", e);
            return SaveResult.error("Unexpected error occurred while saving product");
        }
    }

    private ProductImage parseMultipartFileToUniqueFileName(String uploadDir,
                                                            MultipartFile file,
                                                            boolean isPreviewImage) {
        try {
            if (file != null && !file.isEmpty()) {
                String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
                Path path = Paths.get(uploadDir + fileName);

                try (InputStream inputStream = file.getInputStream()) {
                    Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);
                }

                return new ProductImage(uploadDir + fileName, isPreviewImage);
            }
        } catch (IOException e) {
            log.error("Error saving file: {}", file.getOriginalFilename(), e);
        }
        return null;
    }

    /**
     * Clean up images from disk
     */
    private void cleanupImages(List<ProductImage> images) {
        for (ProductImage image : images) {
            productImageService.deleteImageFromDisk(image);
        }
    }

    @Transactional
    public void deleteProductById(Long id) {
        Product product = productRepository.findById(id).orElse(null);
        if (product == null) {
            log.error("Product with id {} not found", id);
            return;
        }

        for (var image : product.getImages()) {
            productImageService.deleteImageFromDisk(image);
        }

        productRepository.deleteById(id);

        // Delete from Elasticsearch index
        searchService.deleteProductFromIndex(id);
    }

    @Transactional
    public SaveResult updateProduct(Long id,
                                    MultipartFile previewImage,
                                    List<MultipartFile> additionalImages,
                                    List<Long> removeImageIds,
                                    Product product) {

        Product dbProduct = productRepository.findById(id).orElse(null);
        if (dbProduct == null) {
            return SaveResult.error("Product not found");
        }

        // Validate preview image if provided
        if (previewImage != null && !previewImage.isEmpty()) {
            ValidationResult validation = imageValidationService.validateImage(previewImage);
            if (!validation.isValid()) {
                return SaveResult.error(validation.getErrorMessage());
            }
        }

        // Validate additional images if provided
        ValidationResult additionalValidation = imageValidationService.validateAdditionalImages(additionalImages);
        if (!additionalValidation.isValid()) {
            return SaveResult.error(additionalValidation.getErrorMessage());
        }

        try {
            List<ProductImage> newImages = new ArrayList<>();

            dbProduct.setTitle(product.getTitle());
            dbProduct.setDescription(product.getDescription());
            dbProduct.setPrice(product.getPrice());

            // Remove specified images
            if (removeImageIds != null && !removeImageIds.isEmpty()) {
                for (var idRemoveImage : removeImageIds) {
                    ProductImage image = productImageService.getImageById(idRemoveImage);
                    if (image != null) {
                        dbProduct.deleteImage(image);
                        productImageService.deleteImageFromDisk(image);
                    }
                }
            }

            // Replace preview image if new one provided
            if (previewImage != null && !previewImage.isEmpty()) {
                ProductImage currentPreviewImage = dbProduct.getPreviewImage();
                if (currentPreviewImage != null) {
                    dbProduct.deleteImage(currentPreviewImage);
                    productImageService.deleteImageFromDisk(currentPreviewImage);
                }

                ProductImage newPreviewImage = parseMultipartFileToUniqueFileName(
                        ProductImage.DIRECTORY_IMAGES, previewImage, true
                );
                if (newPreviewImage != null) {
                    newPreviewImage.setProduct(dbProduct);
                    newImages.add(newPreviewImage);
                } else {
                    log.error("Failed to save new preview image");
                    return SaveResult.error("Failed to update preview image");
                }
            }

            // Add new additional images
            if (additionalImages != null && !additionalImages.isEmpty()) {
                for (var file : additionalImages) {
                    if (file != null && !file.isEmpty()) {
                        ProductImage newAdditionalImage = parseMultipartFileToUniqueFileName(
                                ProductImage.DIRECTORY_IMAGES, file, false
                        );
                        if (newAdditionalImage != null) {
                            newAdditionalImage.setProduct(dbProduct);
                            newImages.add(newAdditionalImage);
                        } else {
                            log.error("Failed to save new additional image");
                            cleanupImages(newImages);
                            return SaveResult.error("Failed to update additional images");
                        }
                    }
                }
            }

            dbProduct.addImages(newImages);
            productRepository.save(dbProduct);

            // Update in Elasticsearch index
            searchService.indexProduct(dbProduct);

            return SaveResult.success();

        } catch (Exception e) {
            log.error("Error updating product", e);
            return SaveResult.error("Error updating product: " + e.getMessage());
        }
    }

    // Search method
    public Page<Product> searchProducts(String query, int page, int size) {
        return searchService.searchProducts(query, page, size);
    }

    /**
     * Result class for save operations
     */
    public static class SaveResult {
        private final boolean success;
        private final String errorMessage;

        private SaveResult(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }

        public static SaveResult success() {
            return new SaveResult(true, null);
        }

        public static SaveResult error(String message) {
            return new SaveResult(false, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}