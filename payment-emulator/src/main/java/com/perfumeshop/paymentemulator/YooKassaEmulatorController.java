package com.perfumeshop.paymentemulator;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/v3")
public class YooKassaEmulatorController {

    private final EmulatorStore store;
    private final EmulatorProperties properties;
    private final AuthSupport authSupport;

    public YooKassaEmulatorController(
            EmulatorStore store,
            EmulatorProperties properties,
            AuthSupport authSupport
    ) {
        this.store = store;
        this.properties = properties;
        this.authSupport = authSupport;
    }

    @PostMapping("/payments")
    public Map<String, Object> createPayment(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "Idempotence-Key", required = false) String idempotenceKey,
            @RequestBody Map<String, Object> body
    ) {
        authSupport.requireBasicAuth(authorization);
        store.record("POST", "/v3/payments", body);

        @SuppressWarnings("unchecked")
        Map<String, Object> amount = body.get("amount") instanceof Map<?, ?> a
                ? new LinkedHashMap<>((Map<String, Object>) a)
                : Map.of("value", "0.00", "currency", "RUB");
        String description = body.get("description") == null ? null : String.valueOf(body.get("description"));
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = body.get("metadata") instanceof Map<?, ?> m
                ? new LinkedHashMap<>((Map<String, Object>) m)
                : Map.of();

        String id = store.newPaymentId();
        String base = properties.getPublicBaseUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String confirmationUrl = base + "/emulator/confirm/" + id;

        StoredPayment payment = new StoredPayment(
                id,
                "pending",
                amount,
                description,
                metadata,
                confirmationUrl
        );
        store.save(payment);
        return payment.toApiObject();
    }

    @GetMapping("/payments/{id}")
    public Map<String, Object> getPayment(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable String id
    ) {
        authSupport.requireBasicAuth(authorization);
        store.record("GET", "/v3/payments/" + id, Map.of("id", id));
        StoredPayment payment = store.find(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
        return payment.toApiObject();
    }
}
