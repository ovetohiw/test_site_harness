package com.perfumeshop.web;

import com.perfumeshop.dto.catalog.AddToCartRequest;
import com.perfumeshop.dto.catalog.CartResponse;
import com.perfumeshop.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    public static final String CART_ID_HEADER = "X-Cart-Id";

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartResponse getCart(@RequestHeader(CART_ID_HEADER) String cartIdHeader) {
        return cartService.getCart(parseCartId(cartIdHeader));
    }

    @PostMapping("/items")
    public CartResponse addItem(
            @RequestHeader(CART_ID_HEADER) String cartIdHeader,
            @Valid @RequestBody AddToCartRequest request
    ) {
        return cartService.addItem(parseCartId(cartIdHeader), request);
    }

    @DeleteMapping("/items/{productId}")
    public CartResponse removeItem(
            @RequestHeader(CART_ID_HEADER) String cartIdHeader,
            @PathVariable Long productId
    ) {
        return cartService.removeItem(parseCartId(cartIdHeader), productId);
    }

    private static UUID parseCartId(String cartIdHeader) {
        try {
            return UUID.fromString(cartIdHeader);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid X-Cart-Id");
        }
    }
}
