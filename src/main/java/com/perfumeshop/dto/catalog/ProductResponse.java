package com.perfumeshop.dto.catalog;

import com.perfumeshop.domain.product.Product;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String name,
        String brand,
        String imageUrl,
        BigDecimal price,
        String description
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getBrand(),
                product.getImageUrl(),
                product.getPrice(),
                product.getDescription()
        );
    }
}
