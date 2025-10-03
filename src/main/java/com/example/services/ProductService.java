package com.example.services;

import com.example.models.Product;
import com.example.models.ProductImage;
import com.example.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;

    public List<Product> getProducts() {
        return productRepository.findAll();
    }

    public Product getById(Long id) {
        return productRepository.findById(id).orElse(null);
    }

    public void save(Product product) {
        productRepository.save(product);
    }

    public boolean saveProductWithImages(MultipartFile previewImage,
                     List<MultipartFile> additionalImages,
                     Product product) throws IOException {

        if(previewImage == null || previewImage.isEmpty() || product == null) return false;

        String uploadDir = ProductImage.DIRECTORY_IMAGES;
        Files.createDirectories(Paths.get(uploadDir));

        List<ProductImage> images = new ArrayList<>();

        //previewImage
        images.add(parseMultipartFileToUniqueFileName(uploadDir, previewImage, true));

        //additionalImages
        if(additionalImages != null && !additionalImages.isEmpty())
            for (MultipartFile file : additionalImages)
                if(file != null && !file.isEmpty())
                    images.add(parseMultipartFileToUniqueFileName(uploadDir, file, false));


        for (ProductImage img : images)
            img.setProduct(product);

        product.setImages(images);

        productRepository.save(product);

        return true;
    }

    private ProductImage parseMultipartFileToUniqueFileName(String uploadDir, MultipartFile file, boolean isPreviewImage) throws IOException {
        if(file != null && !file.isEmpty()) {
            String fileName = UUID.randomUUID()	 + "_" + file.getOriginalFilename();
            Path path = Paths.get(uploadDir + fileName);
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

            return new ProductImage("/" + uploadDir + fileName, isPreviewImage);
        }
        return null;
    }
}
