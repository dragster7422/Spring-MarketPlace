package com.example.services;

import com.example.models.Product;
import com.example.models.ProductImage;
import com.example.models.User;
import com.example.repositories.ProductRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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

    public List<Product> getProducts() {
        return productRepository.findAll();
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

    public boolean saveProductWithImages(MultipartFile previewImage,
                                         List<MultipartFile> additionalImages,
                                         Product product,
                                         User owner) throws IOException {

        if(previewImage == null || previewImage.isEmpty() || product == null || owner == null) return false;

        String uploadDir = ProductImage.DIRECTORY_IMAGES;
        Files.createDirectories(Paths.get(uploadDir));

        List<ProductImage> images = new ArrayList<>();

        // Preview image
        ProductImage preview = parseMultipartFileToUniqueFileName(uploadDir, previewImage, true);
        if(preview != null) {
            preview.setProduct(product);
            images.add(preview);
        } else {
            log.error("Failed to save preview image");
            return false;
        }

        // Additional images
        if(additionalImages != null && !additionalImages.isEmpty()) {
            for (MultipartFile file : additionalImages) {
                if(file != null && !file.isEmpty()) {
                    ProductImage img = parseMultipartFileToUniqueFileName(uploadDir, file, false);
                    if(img != null) {
                        img.setProduct(product);
                        images.add(img);
                    } else {
                        log.error("Failed to save additional image");
                        return false;
                    }
                }
            }
        }

        product.setImages(images);
        product.setOwner(owner);
        productRepository.save(product);

        // Index product in Elasticsearch
        searchService.indexProduct(product);

        return true;
    }

    private ProductImage parseMultipartFileToUniqueFileName(String uploadDir,
                                                            MultipartFile file,
                                                            boolean isPreviewImage) {
        try {
            if(file != null && !file.isEmpty()) {
                String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
                Path path = Paths.get(uploadDir + fileName);
                Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

                return new ProductImage(uploadDir + fileName, isPreviewImage);
            }
        } catch (IOException e) {
            log.error("Error saving file: {}", file.getOriginalFilename(), e);
        }
        return null;
    }

    @Transactional
    public void deleteProductById(Long id) {
        Product product = productRepository.findById(id).orElse(null);
        if(product == null) {
            log.error("Product with id {} not found", id);
            return;
        }

        for(var image : product.getImages()) {
            productImageService.deleteImageFromDisk(image);
        }

        productRepository.deleteById(id);

        // Delete from Elasticsearch index
        searchService.deleteProductFromIndex(id);
    }

    @Transactional
    public boolean updateProduct(Long id,
                                 MultipartFile previewImage,
                                 List<MultipartFile> additionalImages,
                                 List<Long> removeImageIds,
                                 Product product) {

        Product dbProduct = productRepository.findById(id).orElse(null);
        if(dbProduct == null)
            return false;
        List<ProductImage> newImages = new ArrayList<>();

        dbProduct.setTitle(product.getTitle());
        dbProduct.setDescription(product.getDescription());
        dbProduct.setPrice(product.getPrice());

        if(removeImageIds != null && !removeImageIds.isEmpty()) {
            for(var idRemoveImage : removeImageIds) {
                ProductImage image = productImageService.getImageById(idRemoveImage);
                dbProduct.deleteImage(image);
                productImageService.deleteImageFromDisk(image);
            }
        }

        if(previewImage != null && !previewImage.isEmpty()) {
            ProductImage currentPreviewImage = dbProduct.getPreviewImage();
            dbProduct.deleteImage(currentPreviewImage);
            productImageService.deleteImageFromDisk(currentPreviewImage);

            ProductImage newPreviewImage = parseMultipartFileToUniqueFileName(ProductImage.DIRECTORY_IMAGES, previewImage, true);
            if(newPreviewImage != null) {
                newPreviewImage.setProduct(dbProduct);
                newImages.add(newPreviewImage);
            } else {
                log.error("Failed to save new preview image");
                return false;
            }
        }

        if(additionalImages != null && !additionalImages.isEmpty()) {
            for(var file : additionalImages) {
                if(file != null && !file.isEmpty()) {
                    ProductImage newAdditionalImage = parseMultipartFileToUniqueFileName(ProductImage.DIRECTORY_IMAGES, file, false);
                    if(newAdditionalImage != null) {
                        newAdditionalImage.setProduct(dbProduct);
                        newImages.add(newAdditionalImage);
                    } else {
                        log.error("Failed to save new additional images");
                        return false;
                    }
                }
            }
        }

        dbProduct.addImages(newImages);
        productRepository.save(dbProduct);

        // Update in Elasticsearch index
        searchService.indexProduct(dbProduct);

        return true;
    }

    // Search method
    public List<Product> searchProducts(String query) {
        return searchService.searchProducts(query);
    }
}
