package com.perfumeshop.service;

import com.perfumeshop.domain.cart.Cart;
import com.perfumeshop.domain.cart.CartItem;
import com.perfumeshop.domain.product.Product;
import com.perfumeshop.dto.catalog.AddToCartRequest;
import com.perfumeshop.dto.catalog.CartItemResponse;
import com.perfumeshop.dto.catalog.CartResponse;
import com.perfumeshop.repository.CartRepository;
import com.perfumeshop.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.UUID;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    public CartService(CartRepository cartRepository, ProductRepository productRepository) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(UUID cartId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseGet(() -> new Cart(cartId));
        return toResponse(cart);
    }

    @Transactional
    public CartResponse addItem(UUID cartId, AddToCartRequest request) {
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        Cart cart = cartRepository.findById(cartId).orElseGet(() -> cartRepository.save(new Cart(cartId)));
        int quantity = request.resolvedQuantity();

        CartItem existing = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(product.getId()))
                .findFirst()
                .orElse(null);

        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + quantity);
        } else {
            cart.getItems().add(new CartItem(cart, product, quantity));
        }

        return toResponse(cart);
    }

    @Transactional
    public CartResponse removeItem(UUID cartId, Long productId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found"));

        boolean removed = cart.getItems().removeIf(item -> item.getProduct().getId().equals(productId));
        if (!removed) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found");
        }

        return toResponse(cart);
    }

    private CartResponse toResponse(Cart cart) {
        var items = cart.getItems().stream()
                .sorted(Comparator.comparing(item -> item.getProduct().getId()))
                .map(item -> {
                    Product product = item.getProduct();
                    BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                    return new CartItemResponse(
                            product.getId(),
                            product.getName(),
                            product.getImageUrl(),
                            product.getPrice(),
                            product.getDescription(),
                            item.getQuantity(),
                            lineTotal
                    );
                })
                .toList();

        BigDecimal total = items.stream()
                .map(CartItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponse(cart.getId(), items, total);
    }
}
