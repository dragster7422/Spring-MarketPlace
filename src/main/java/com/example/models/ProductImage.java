package com.example.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.*;

@Entity(name = "product_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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

    @Override
    public boolean equals(Object obj) {
        if(this == obj) return true;
        if(obj == null || getClass() != obj.getClass()) return false;
        ProductImage productImage = (ProductImage) obj;
        return id != null && id.equals(productImage.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
