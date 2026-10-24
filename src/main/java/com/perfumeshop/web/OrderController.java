package com.perfumeshop.web;

import com.perfumeshop.dto.catalog.OrderResponse;
import com.perfumeshop.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<OrderResponse> list(Authentication authentication) {
        return orderService.list(authentication.getName());
    }

    @PostMapping("/checkout")
    public OrderResponse checkout(
            Authentication authentication,
            @RequestHeader(CartController.CART_ID_HEADER) String cartIdHeader
    ) {
        return orderService.checkout(authentication.getName(), parseCartId(cartIdHeader));
    }

    private static UUID parseCartId(String cartIdHeader) {
        try {
            return UUID.fromString(cartIdHeader);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid X-Cart-Id");
        }
    }
}
