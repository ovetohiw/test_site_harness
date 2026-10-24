package com.perfumeshop.crmemulator;

import java.time.Instant;
import java.util.Map;

public record EmulatorEvent(
        long id,
        String provider,
        String method,
        String path,
        Map<String, Object> payload,
        Instant receivedAt
) {
}
