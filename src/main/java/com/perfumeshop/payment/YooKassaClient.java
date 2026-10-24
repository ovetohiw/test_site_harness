package com.perfumeshop.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * YooKassa API v3 client. Without shopId/secretKey operates in stub mode
 * (local SUCCEEDED payments, no HTTP).
 */
@Component
public class YooKassaClient {

    private static final Logger log = LoggerFactory.getLogger(YooKassaClient.class);

    private final YooKassaProperties props;
    private final RestClient restClient;

    public YooKassaClient(YooKassaProperties props, RestClient.Builder restClientBuilder) {
        this.props = props;
        this.restClient = restClientBuilder.build();
    }

    public boolean isEnabled() {
        return props.isEnabled();
    }

    public boolean isLive() {
        return props.isLive();
    }

    public String mode() {
        return props.mode();
    }

    public YooKassaPaymentResult createPayment(
            BigDecimal amount,
            String description,
            Map<String, String> metadata
    ) {
        if (!props.isLive()) {
            return new YooKassaPaymentResult(
                    "stub-" + UUID.randomUUID(),
                    PaymentStatus.SUCCEEDED,
                    amount.setScale(2, RoundingMode.HALF_UP),
                    "RUB",
                    null,
                    description
            );
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("amount", Map.of(
                "value", amount.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                "currency", "RUB"
        ));
        body.put("capture", true);
        body.put("description", description);
        body.put("confirmation", Map.of(
                "type", "redirect",
                "return_url", props.getReturnUrl()
        ));
        if (metadata != null && !metadata.isEmpty()) {
            body.put("metadata", metadata);
        }

        Map<?, ?> response = post("/payments", body, UUID.randomUUID().toString());
        return mapPayment(response);
    }

    public YooKassaPaymentResult getPayment(String paymentId) {
        if (!props.isLive()) {
            return new YooKassaPaymentResult(
                    paymentId,
                    PaymentStatus.SUCCEEDED,
                    BigDecimal.ZERO,
                    "RUB",
                    null,
                    "stub"
            );
        }
        Map<?, ?> response = get("/payments/" + paymentId);
        return mapPayment(response);
    }

    @SuppressWarnings("unchecked")
    private Map<?, ?> post(String path, Map<String, Object> body, String idempotenceKey) {
        try {
            Map<?, ?> response = restClient.post()
                    .uri(apiUrl(path))
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", basicAuth())
                    .header("Idempotence-Key", idempotenceKey)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            if (response == null) {
                throw new RestClientException("Empty YooKassa response for POST " + path);
            }
            return response;
        } catch (RestClientException ex) {
            log.warn("YooKassa POST {} failed: {}", path, ex.getMessage());
            throw ex;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<?, ?> get(String path) {
        try {
            Map<?, ?> response = restClient.get()
                    .uri(apiUrl(path))
                    .header("Authorization", basicAuth())
                    .retrieve()
                    .body(Map.class);
            if (response == null) {
                throw new RestClientException("Empty YooKassa response for GET " + path);
            }
            return response;
        } catch (RestClientException ex) {
            log.warn("YooKassa GET {} failed: {}", path, ex.getMessage());
            throw ex;
        }
    }

    private String apiUrl(String path) {
        String base = props.getBaseUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return base + path;
    }

    private String basicAuth() {
        String raw = props.getShopId() + ":" + props.getSecretKey();
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    @SuppressWarnings("unchecked")
    public static YooKassaPaymentResult mapPayment(Map<?, ?> response) {
        String id = String.valueOf(response.get("id"));
        PaymentStatus status = parseStatus(String.valueOf(response.get("status")));
        Map<String, Object> amountMap = (Map<String, Object>) response.get("amount");
        BigDecimal amount = BigDecimal.ZERO;
        String currency = "RUB";
        if (amountMap != null) {
            Object value = amountMap.get("value");
            if (value != null) {
                amount = new BigDecimal(String.valueOf(value));
            }
            Object curr = amountMap.get("currency");
            if (curr != null) {
                currency = String.valueOf(curr);
            }
        }
        String confirmationUrl = null;
        Object confirmation = response.get("confirmation");
        if (confirmation instanceof Map<?, ?> conf) {
            Object url = conf.get("confirmation_url");
            if (url != null) {
                confirmationUrl = String.valueOf(url);
            }
        }
        String description = response.get("description") == null
                ? null
                : String.valueOf(response.get("description"));
        return new YooKassaPaymentResult(id, status, amount, currency, confirmationUrl, description);
    }

    static PaymentStatus parseStatus(String raw) {
        if (raw == null) {
            return PaymentStatus.PENDING;
        }
        return switch (raw.toLowerCase()) {
            case "succeeded" -> PaymentStatus.SUCCEEDED;
            case "canceled", "cancelled" -> PaymentStatus.CANCELED;
            case "waiting_for_capture" -> PaymentStatus.WAITING_FOR_CAPTURE;
            default -> PaymentStatus.PENDING;
        };
    }
}
