package com.perfumeshop.dto.payment;

import com.perfumeshop.domain.payment.Payment;
import com.perfumeshop.payment.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        Long id,
        Long orderId,
        String externalId,
        PaymentStatus status,
        BigDecimal amount,
        String currency,
        String confirmationUrl,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrder().getId(),
                payment.getExternalId(),
                payment.getStatus(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getConfirmationUrl(),
                payment.getDescription(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
