package com.perfumeshop.dto.catalog;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddToCartRequest(
        @NotNull Long productId,
        @Positive Integer quantity
) {
    public int resolvedQuantity() {
        return quantity == null ? 1 : quantity;
    }
}
