package com.perfumeshop.dto.catalog;

import com.perfumeshop.domain.order.ShopOrder;
import com.perfumeshop.domain.payment.Payment;
import com.perfumeshop.payment.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        BigDecimal total,
        Instant createdAt,
        PaymentStatus paymentStatus,
        String paymentExternalId,
        String confirmationUrl,
        List<OrderItemResponse> items
) {
    public static OrderResponse from(ShopOrder order) {
        return from(order, null);
    }

    public static OrderResponse from(ShopOrder order, Payment payment) {
        return new OrderResponse(
                order.getId(),
                order.getTotal(),
                order.getCreatedAt(),
                order.getPaymentStatus(),
                payment == null ? null : payment.getExternalId(),
                payment == null ? null : payment.getConfirmationUrl(),
                order.getItems().stream().map(OrderItemResponse::from).toList()
        );
    }
}
