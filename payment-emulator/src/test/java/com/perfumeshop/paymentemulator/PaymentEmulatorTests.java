package com.perfumeshop.paymentemulator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentEmulatorTests {

    @LocalServerPort
    private int port;

    @Autowired
    private EmulatorStore store;

    private TestRestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        store.clear();
        restTemplate = new TestRestTemplate();
        restTemplate.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
    }

    @Test
    void healthIsUp() {
        ResponseEntity<Map> health = restTemplate.getForEntity(url("/actuator/health"), Map.class);
        assertThat(health.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(health.getBody().get("status")).isEqualTo("UP");
    }

    @Test
    void createAndGetPaymentRequiresAuthAndReturnsPending() {
        ResponseEntity<String> unauthorized = restTemplate.postForEntity(
                url("/v3/payments"),
                Map.of("amount", Map.of("value", "10.00", "currency", "RUB")),
                String.class
        );
        assertThat(unauthorized.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        ResponseEntity<Map> created = restTemplate.exchange(
                url("/v3/payments"),
                HttpMethod.POST,
                new HttpEntity<>(
                        Map.of(
                                "amount", Map.of("value", "1990.00", "currency", "RUB"),
                                "description", "Order #1",
                                "capture", true,
                                "confirmation", Map.of("type", "redirect", "return_url", "http://localhost:8080/"),
                                "metadata", Map.of("orderId", "1")
                        ),
                        authHeaders()
                ),
                Map.class
        );
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(created.getBody().get("status")).isEqualTo("pending");
        assertThat(created.getBody().get("id")).isNotNull();
        Map<?, ?> confirmation = (Map<?, ?>) created.getBody().get("confirmation");
        assertThat(confirmation.get("confirmation_url")).asString().contains("/emulator/confirm/");

        String id = String.valueOf(created.getBody().get("id"));
        ResponseEntity<Map> fetched = restTemplate.exchange(
                url("/v3/payments/" + id),
                HttpMethod.GET,
                new HttpEntity<>(authHeaders()),
                Map.class
        );
        assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(fetched.getBody().get("id")).isEqualTo(id);
        assertThat(fetched.getBody().get("status")).isEqualTo("pending");
    }

    @Test
    void succeedEndpointMarksPaymentPaidAndInspectApisWork() {
        ResponseEntity<Map> created = restTemplate.exchange(
                url("/v3/payments"),
                HttpMethod.POST,
                new HttpEntity<>(
                        Map.of("amount", Map.of("value", "100.00", "currency", "RUB")),
                        authHeaders()
                ),
                Map.class
        );
        String id = String.valueOf(created.getBody().get("id"));

        ResponseEntity<Map> succeeded = restTemplate.postForEntity(
                url("/emulator/payments/" + id + "/succeed"),
                null,
                Map.class
        );
        assertThat(succeeded.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(succeeded.getBody().get("status")).isEqualTo("succeeded");
        assertThat(succeeded.getBody().get("paid")).isEqualTo(true);

        ResponseEntity<Map> status = restTemplate.getForEntity(url("/emulator/status"), Map.class);
        assertThat(status.getBody().get("paymentCount")).isEqualTo(1);
        assertThat(((Number) status.getBody().get("eventCount")).intValue()).isGreaterThanOrEqualTo(2);

        ResponseEntity<List> events = restTemplate.getForEntity(url("/emulator/events"), List.class);
        assertThat(events.getBody()).isNotEmpty();

        restTemplate.delete(url("/emulator/events"));
        ResponseEntity<Map> cleared = restTemplate.getForEntity(url("/emulator/status"), Map.class);
        assertThat(cleared.getBody().get("eventCount")).isEqualTo(0);
        assertThat(cleared.getBody().get("paymentCount")).isEqualTo(0);
    }

    @Test
    void wrongCredentialsRejected() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Basic " + Base64.getEncoder()
                .encodeToString("wrong:creds".getBytes(StandardCharsets.UTF_8)));
        headers.set("Idempotence-Key", UUID.randomUUID().toString());

        ResponseEntity<String> response = restTemplate.exchange(
                url("/v3/payments"),
                HttpMethod.POST,
                new HttpEntity<>(Map.of("amount", Map.of("value", "1.00", "currency", "RUB")), headers),
                String.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String raw = "test-shop:test-secret";
        headers.set("Authorization", "Basic " + Base64.getEncoder()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8)));
        headers.set("Idempotence-Key", UUID.randomUUID().toString());
        return headers;
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
