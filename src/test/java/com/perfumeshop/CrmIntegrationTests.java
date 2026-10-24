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
class CrmIntegrationTests {

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
    void crmStatusShowsBitrixAndAmoInStubMode() {
        String token = register("crm_status_" + UUID.randomUUID().toString().substring(0, 8), "secret12");

        ResponseEntity<Map> status = restTemplate.exchange(
                url("/api/crm/status"),
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(token)),
                Map.class
        );

        assertThat(status.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(status.getBody().get("enabled")).isEqualTo(true);
        List<Map<String, Object>> providers = (List<Map<String, Object>>) status.getBody().get("providers");
        assertThat(providers).extracting(p -> p.get("name"))
                .containsExactlyInAnyOrder("BITRIX24", "AMOCRM");
        assertThat(providers).allSatisfy(p -> {
            assertThat(p.get("enabled")).isEqualTo(true);
            assertThat(p.get("mode")).isEqualTo("stub");
        });
    }

    @Test
    void registerCheckoutAndFavoriteCreateCrmEventsForBothProviders() {
        String login = "crm_buyer_" + UUID.randomUUID().toString().substring(0, 8);
        String token = register(login, "secret12");
        Long productId = firstProductId();
        String cartId = UUID.randomUUID().toString();

        restTemplate.exchange(
                url("/api/favorites"),
                HttpMethod.POST,
                new HttpEntity<>(Map.of("productId", productId), jsonAuth(token)),
                new ParameterizedTypeReference<List<Map<String, Object>>>() {
                }
        );

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

        ResponseEntity<List<Map<String, Object>>> events = restTemplate.exchange(
                url("/api/crm/events"),
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(token)),
                new ParameterizedTypeReference<>() {
                }
        );
        assertThat(events.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(events.getBody()).isNotNull();

        List<Map<String, Object>> mine = events.getBody().stream()
                .filter(e -> login.toLowerCase().equals(e.get("userLogin")))
                .toList();

        assertThat(mine).anyMatch(e ->
                "BITRIX24".equals(e.get("provider"))
                        && "CUSTOMER_REGISTERED".equals(e.get("eventType"))
                        && "SUCCESS".equals(e.get("status")));
        assertThat(mine).anyMatch(e ->
                "AMOCRM".equals(e.get("provider"))
                        && "CUSTOMER_REGISTERED".equals(e.get("eventType"))
                        && "SUCCESS".equals(e.get("status")));
        assertThat(mine).anyMatch(e ->
                "BITRIX24".equals(e.get("provider"))
                        && "SALE_COMPLETED".equals(e.get("eventType"))
                        && "SUCCESS".equals(e.get("status")));
        assertThat(mine).anyMatch(e ->
                "AMOCRM".equals(e.get("provider"))
                        && "SALE_COMPLETED".equals(e.get("eventType"))
                        && "SUCCESS".equals(e.get("status")));
        assertThat(mine).anyMatch(e ->
                "BITRIX24".equals(e.get("provider"))
                        && "FAVORITE_ADDED".equals(e.get("eventType"))
                        && "SUCCESS".equals(e.get("status")));
        assertThat(mine).anyMatch(e ->
                "AMOCRM".equals(e.get("provider"))
                        && "FAVORITE_ADDED".equals(e.get("eventType"))
                        && "SUCCESS".equals(e.get("status")));

        Integer dbCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM crm_sync_events WHERE user_login = ? AND status = 'SUCCESS'",
                Integer.class,
                login.toLowerCase()
        );
        assertThat(dbCount).isGreaterThanOrEqualTo(6);
    }

    @Test
    void crmEndpointsRequireAuth() {
        ResponseEntity<String> status = restTemplate.getForEntity(url("/api/crm/status"), String.class);
        assertThat(status.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        ResponseEntity<String> events = restTemplate.getForEntity(url("/api/crm/events"), String.class);
        assertThat(events.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
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

    private HttpHeaders jsonAuth(String token) {
        HttpHeaders headers = bearerHeaders(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
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
