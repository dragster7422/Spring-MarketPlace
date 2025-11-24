package com.example.repositories;

import com.example.models.Product;
import com.example.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByOwnerId(Long id);
}
