package com.perfumeshop.service;

import com.perfumeshop.domain.cart.Cart;
import com.perfumeshop.domain.cart.CartItem;
import com.perfumeshop.domain.order.OrderItem;
import com.perfumeshop.domain.order.ShopOrder;
import com.perfumeshop.domain.product.Product;
import com.perfumeshop.domain.user.User;
import com.perfumeshop.dto.catalog.OrderResponse;
import com.perfumeshop.repository.CartRepository;
import com.perfumeshop.repository.OrderRepository;
import com.perfumeshop.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final CrmService crmService;

    public OrderService(
            OrderRepository orderRepository,
            CartRepository cartRepository,
            UserRepository userRepository,
            CrmService crmService
    ) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.userRepository = userRepository;
        this.crmService = crmService;
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> list(String login) {
        User user = requireUser(login);
        return orderRepository.findByUser_IdOrderByCreatedAtDesc(user.getId()).stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional
    public OrderResponse checkout(String login, UUID cartId) {
        User user = requireUser(login);
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart is empty"));

        if (cart.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart is empty");
        }

        BigDecimal total = cart.getItems().stream()
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        ShopOrder order = new ShopOrder(user, total);
        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            order.addItem(new OrderItem(product, cartItem.getQuantity()));
        }

        ShopOrder saved = orderRepository.save(order);
        cart.getItems().clear();
        cartRepository.save(cart);
        crmService.onSaleCompleted(saved);

        return OrderResponse.from(saved);
    }

    private User requireUser(String login) {
        return userRepository.findByLogin(login)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));
    }
}
