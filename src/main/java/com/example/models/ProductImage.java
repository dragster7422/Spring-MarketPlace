package com.example.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity(name = "product_images")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductImage {

    public static final String DIRECTORY_IMAGES = "uploads/";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String imageDirectory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column
    private boolean isPreviewImage = false;

    public ProductImage(String imageDirectory, boolean isPreviewImage) {
        this.imageDirectory = imageDirectory;
        this.isPreviewImage = isPreviewImage;
    }

    public String getImageUrl() {
        if (imageDirectory.startsWith("http://") || imageDirectory.startsWith("https://")) {
            return imageDirectory;
        }

        return "/" + imageDirectory;
    }
}
