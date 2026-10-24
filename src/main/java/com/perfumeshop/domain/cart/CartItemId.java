package com.perfumeshop.domain.cart;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class CartItemId implements Serializable {

    private UUID cart;
    private Long product;

    public CartItemId() {
    }

    public CartItemId(UUID cart, Long product) {
        this.cart = cart;
        this.product = product;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CartItemId that)) {
            return false;
        }
        return Objects.equals(cart, that.cart) && Objects.equals(product, that.product);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cart, product);
    }
}
