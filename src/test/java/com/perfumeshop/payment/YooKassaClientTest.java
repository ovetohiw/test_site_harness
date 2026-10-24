package com.perfumeshop.payment;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class YooKassaClientTest {

    @Test
    void stubCreatePaymentReturnsSucceededWithoutHttp() {
        YooKassaProperties props = new YooKassaProperties();
        props.setShopId("");
        props.setSecretKey("");
        YooKassaClient client = new YooKassaClient(props, org.springframework.web.client.RestClient.builder());

        YooKassaPaymentResult result = client.createPayment(
                new BigDecimal("1500.00"),
                "Order #1",
                Map.of("orderId", "1")
        );

        assertThat(client.mode()).isEqualTo("stub");
        assertThat(result.status()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(result.id()).startsWith("stub-");
        assertThat(result.amount()).isEqualByComparingTo("1500.00");
        assertThat(result.confirmationUrl()).isNull();
    }

    @Test
    void mapPaymentParsesYooKassaPayload() {
        Map<String, Object> payload = Map.of(
                "id", "pay-123",
                "status", "pending",
                "amount", Map.of("value", "99.50", "currency", "RUB"),
                "description", "Test",
                "confirmation", Map.of(
                        "type", "redirect",
                        "confirmation_url", "https://example.test/pay"
                )
        );

        YooKassaPaymentResult result = YooKassaClient.mapPayment(payload);
        assertThat(result.id()).isEqualTo("pay-123");
        assertThat(result.status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(result.amount()).isEqualByComparingTo("99.50");
        assertThat(result.confirmationUrl()).isEqualTo("https://example.test/pay");
    }
}
