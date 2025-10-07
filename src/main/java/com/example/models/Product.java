package com.example.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity(name = "products")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "products_seq")
    private Long id;

    @Column
    @NotBlank(message = "The title cannot be empty")
    private String title;

    @Column
    @NotBlank(message = "The description cannot be empty")
    @Size(min = 20, message = "The description must be more than 20 characters")
    private String description;

    @Column
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0", message = "Price must be greater than 0")
    private Double price;

    @Column
    private LocalDateTime dateOfCreated;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ProductImage> images;

    @PrePersist
    private void init() {
        dateOfCreated = LocalDateTime.now();
    }
}
