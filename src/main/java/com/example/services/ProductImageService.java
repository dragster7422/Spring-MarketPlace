package com.example.services;

import com.example.models.ProductImage;
import com.example.repositories.ProductImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductImageService {
    private final ProductImageRepository productImageRepository;

    public ProductImage getImageById(Long id) {
        return productImageRepository.findById(id).orElse(null);
    }

    public void deleteImageById(Long id) {
        productImageRepository.deleteById(id);
    }

    public void deleteImageFromDisk(ProductImage image) {
        if(image != null && image.getImageDirectory() != null) {
            String imageDir = image.getImageDirectory();

            try {
                Files.delete(Paths.get(imageDir));
            } catch (NoSuchFileException e) {
                log.error("file " + imageDir + " does not exist");
            } catch (IOException e) {
                log.error("file " + imageDir + " could not be deleted\n" + e.getMessage());
            }
        }
    }
}
