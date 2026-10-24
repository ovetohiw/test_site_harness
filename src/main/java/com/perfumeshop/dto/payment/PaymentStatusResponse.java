package com.perfumeshop.dto.payment;

public record PaymentStatusResponse(
        boolean enabled,
        String mode,
        String provider
) {
}
