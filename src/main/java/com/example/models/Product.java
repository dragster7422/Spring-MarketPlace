package com.example.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "products_seq")
    @SequenceGenerator(name = "products_seq", sequenceName = "products_seq_id", allocationSize = 1)
    private Long id;

    @Column
    @NotBlank(message = "The title cannot be empty")
    private String title;

    @Column
    @NotBlank(message = "The description cannot be empty")
    @Size(min = 20, message = "The description must be more than 20 characters")
    private String description;

    @Column(precision = 10, scale = 2)
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", message = "Price must be greater than 0")
    private BigDecimal price;

    @Column
    private LocalDateTime dateOfCreated;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ProductImage> images;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User owner;

    @PrePersist
    private void init() {
        dateOfCreated = LocalDateTime.now();
    }

    public ProductImage getPreviewImage() {
        if (images == null || images.isEmpty()) {
            return null;
        }
        return images.stream()
                .filter(ProductImage::isPreviewImage)
                .findFirst()
                .get();
    }

    public String getPreviewImageUrl() {
        return getPreviewImage().getImageUrl();
    }

    public void addImages(List<ProductImage> newImages) {
        this.images.addAll(newImages);
    }

    public void deleteImage(ProductImage image) {
        this.images.remove(image);
    }

    public List<ProductImage> getSortedImages() {
        if(images == null || images.isEmpty()) {
            return List.of();
        }

        return images.stream()
                .sorted((img1, img2) -> {
                    if(img1.isPreviewImage()) return -1;
                    if(img2.isPreviewImage()) return 1;
                    return 0;
                })
                .toList();
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) return true;
        if(obj == null || getClass() != obj.getClass()) return false;
        Product product = (Product) obj;
        return id != null && id.equals(product.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
