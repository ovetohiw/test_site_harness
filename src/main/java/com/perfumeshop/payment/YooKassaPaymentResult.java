package com.perfumeshop.payment;

import java.math.BigDecimal;

public record YooKassaPaymentResult(
        String id,
        PaymentStatus status,
        BigDecimal amount,
        String currency,
        String confirmationUrl,
        String description
) {
}
