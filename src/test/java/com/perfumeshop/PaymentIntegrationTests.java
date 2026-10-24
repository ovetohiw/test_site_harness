package com.perfumeshop;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentIntegrationTests {

    @LocalServerPort
    private int port;

    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        restTemplate = new TestRestTemplate();
        restTemplate.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
    }

    @Test
    void paymentStatusShowsYookassaStubMode() {
        String token = register("pay_status_" + UUID.randomUUID().toString().substring(0, 8), "secret12");

        ResponseEntity<Map> status = restTemplate.exchange(
                url("/api/payments/status"),
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(token)),
                Map.class
        );

        assertThat(status.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(status.getBody().get("enabled")).isEqualTo(true);
        assertThat(status.getBody().get("mode")).isEqualTo("stub");
        assertThat(status.getBody().get("provider")).isEqualTo("YOOKASSA");
    }

    @Test
    void checkoutCreatesSucceededPaymentInStubMode() {
        String login = "pay_buyer_" + UUID.randomUUID().toString().substring(0, 8);
        String token = register(login, "secret12");
        Long productId = firstProductId();
        String cartId = UUID.randomUUID().toString();

        HttpHeaders cartAuth = cartAndAuthHeaders(cartId, token);
        restTemplate.exchange(
                url("/api/cart/items"),
                HttpMethod.POST,
                new HttpEntity<>(Map.of("productId", productId, "quantity", 1), cartAuth),
                Map.class
        );

        ResponseEntity<Map> checkout = restTemplate.exchange(
                url("/api/orders/checkout"),
                HttpMethod.POST,
                new HttpEntity<>(cartAuth),
                Map.class
        );
        assertThat(checkout.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(checkout.getBody().get("paymentStatus")).isEqualTo("SUCCEEDED");
        assertThat(checkout.getBody().get("paymentExternalId")).asString().startsWith("stub-");
        assertThat(checkout.getBody().get("confirmationUrl")).isNull();

        Integer paymentCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payments p JOIN orders o ON o.id = p.order_id "
                        + "JOIN users u ON u.id = o.user_id WHERE u.login = ? AND p.status = 'SUCCEEDED'",
                Integer.class,
                login.toLowerCase()
        );
        assertThat(paymentCount).isEqualTo(1);

        ResponseEntity<List<Map<String, Object>>> payments = restTemplate.exchange(
                url("/api/payments"),
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(token)),
                new ParameterizedTypeReference<>() {
                }
        );
        assertThat(payments.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(payments.getBody()).anyMatch(p ->
                "SUCCEEDED".equals(p.get("status"))
                        && String.valueOf(p.get("externalId")).startsWith("stub-")
        );
    }

    @Test
    void webhookMarksPendingPaymentSucceeded() {
        String login = "pay_hook_" + UUID.randomUUID().toString().substring(0, 8);
        String token = register(login, "secret12");
        Long productId = firstProductId();
        String cartId = UUID.randomUUID().toString();

        // Force a pending row via stub checkout then rewrite status for webhook test
        HttpHeaders cartAuth = cartAndAuthHeaders(cartId, token);
        restTemplate.exchange(
                url("/api/cart/items"),
                HttpMethod.POST,
                new HttpEntity<>(Map.of("productId", productId, "quantity", 1), cartAuth),
                Map.class
        );
        ResponseEntity<Map> checkout = restTemplate.exchange(
                url("/api/orders/checkout"),
                HttpMethod.POST,
                new HttpEntity<>(cartAuth),
                Map.class
        );
        String externalId = String.valueOf(checkout.getBody().get("paymentExternalId"));
        Number orderId = (Number) checkout.getBody().get("id");

        jdbcTemplate.update(
                "UPDATE payments SET status = 'PENDING' WHERE external_id = ?",
                externalId
        );
        jdbcTemplate.update(
                "UPDATE orders SET payment_status = 'PENDING' WHERE id = ?",
                orderId.longValue()
        );

        ResponseEntity<Map> webhook = restTemplate.postForEntity(
                url("/api/payments/webhook"),
                Map.of(
                        "type", "notification",
                        "event", "payment.succeeded",
                        "object", Map.of(
                                "id", externalId,
                                "status", "succeeded",
                                "amount", Map.of("value", "100.00", "currency", "RUB"),
                                "description", "test"
                        )
                ),
                Map.class
        );
        assertThat(webhook.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(webhook.getBody().get("status")).isEqualTo("ok");

        String dbStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM payments WHERE external_id = ?",
                String.class,
                externalId
        );
        assertThat(dbStatus).isEqualTo("SUCCEEDED");

        String orderStatus = jdbcTemplate.queryForObject(
                "SELECT payment_status FROM orders WHERE id = ?",
                String.class,
                orderId.longValue()
        );
        assertThat(orderStatus).isEqualTo("SUCCEEDED");
    }

    @Test
    void paymentEndpointsRequireAuthExceptWebhook() {
        ResponseEntity<String> status = restTemplate.getForEntity(url("/api/payments/status"), String.class);
        assertThat(status.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        ResponseEntity<Map> webhook = restTemplate.postForEntity(
                url("/api/payments/webhook"),
                Map.of("type", "notification", "event", "payment.succeeded", "object", Map.of("id", "missing")),
                Map.class
        );
        assertThat(webhook.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private Long firstProductId() {
        ResponseEntity<List<Map<String, Object>>> productsResponse = restTemplate.exchange(
                url("/api/products"),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                }
        );
        return ((Number) productsResponse.getBody().get(0).get("id")).longValue();
    }

    private String register(String login, String password) {
        ResponseEntity<Map> registerResponse = restTemplate.postForEntity(
                url("/api/auth/register"),
                Map.of("login", login, "password", password),
                Map.class
        );
        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return (String) registerResponse.getBody().get("token");
    }

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private HttpHeaders cartAndAuthHeaders(String cartId, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Cart-Id", cartId);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
