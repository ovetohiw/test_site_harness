package com.perfumeshop.dto.catalog;

import java.math.BigDecimal;

public record CartItemResponse(
        Long productId,
        String name,
        String imageUrl,
        BigDecimal price,
        String description,
        int quantity,
        BigDecimal lineTotal
) {
}
