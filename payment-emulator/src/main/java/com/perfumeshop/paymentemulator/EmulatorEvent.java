package com.perfumeshop.paymentemulator;

import java.time.Instant;
import java.util.Map;

public record EmulatorEvent(
        long id,
        String method,
        String path,
        Map<String, Object> payload,
        Instant at
) {
}
