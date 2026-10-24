package com.perfumeshop.dto.catalog;

import jakarta.validation.constraints.NotNull;

public record FavoriteRequest(@NotNull Long productId) {
}
