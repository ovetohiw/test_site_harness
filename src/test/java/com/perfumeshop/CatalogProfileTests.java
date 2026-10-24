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
class CatalogProfileTests {

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
    void productsSortByPriceAscending() {
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url("/api/products?sort=price&dir=asc"),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                }
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> products = response.getBody();
        assertThat(products).isNotNull().hasSizeGreaterThanOrEqualTo(2);

        double previous = Double.NEGATIVE_INFINITY;
        for (Map<String, Object> product : products) {
            double price = ((Number) product.get("price")).doubleValue();
            assertThat(price).isGreaterThanOrEqualTo(previous);
            previous = price;
            assertThat(product.get("brand")).asString().isNotBlank();
        }
    }

    @Test
    void productsSortByBrandDescending() {
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url("/api/products?sort=brand&dir=desc"),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                }
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<String> brands = response.getBody().stream()
                .map(product -> String.valueOf(product.get("brand")))
                .toList();
        assertThat(brands).isSortedAccordingTo((a, b) -> b.compareToIgnoreCase(a));
    }

    @Test
    void addMultipleQuantityToCart() {
        String cartId = UUID.randomUUID().toString();
        Long productId = firstProductId();

        HttpHeaders headers = cartHeaders(cartId);
        ResponseEntity<Map> addResponse = restTemplate.exchange(
                url("/api/cart/items"),
                HttpMethod.POST,
                new HttpEntity<>(Map.of("productId", productId, "quantity", 3), headers),
                Map.class
        );

        assertThat(addResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> items = (List<Map<String, Object>>) addResponse.getBody().get("items");
        assertThat(items).hasSize(1);
        assertThat(((Number) items.get(0).get("quantity")).intValue()).isEqualTo(3);
    }

    @Test
    void authenticatedProfileFavoritesAndOrders() {
        String login = "buyer_" + UUID.randomUUID().toString().substring(0, 8);
        String password = "secret12";
        String token = register(login, password);
        Long productId = firstProductId();
        String cartId = UUID.randomUUID().toString();

        HttpHeaders auth = bearerHeaders(token);
        ResponseEntity<Map> me = restTemplate.exchange(
                url("/api/auth/me"),
                HttpMethod.GET,
                new HttpEntity<>(auth),
                Map.class
        );
        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(me.getBody().get("login")).isEqualTo(login.toLowerCase());
        assertThat(me.getBody().get("avatarUrl")).asString().contains("/images/");
        assertThat(me.getBody().get("createdAt")).isNotNull();

        ResponseEntity<List<Map<String, Object>>> favorites = restTemplate.exchange(
                url("/api/favorites"),
                HttpMethod.POST,
                new HttpEntity<>(Map.of("productId", productId), jsonAuth(token)),
                new ParameterizedTypeReference<>() {
                }
        );
        assertThat(favorites.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(favorites.getBody()).hasSize(1);
        assertThat(favorites.getBody().get(0).get("id")).isEqualTo(productId.intValue());

        HttpHeaders cartAuth = cartAndAuthHeaders(cartId, token);
        restTemplate.exchange(
                url("/api/cart/items"),
                HttpMethod.POST,
                new HttpEntity<>(Map.of("productId", productId, "quantity", 2), cartAuth),
                Map.class
        );

        ResponseEntity<Map> checkout = restTemplate.exchange(
                url("/api/orders/checkout"),
                HttpMethod.POST,
                new HttpEntity<>(cartAuth),
                Map.class
        );
        assertThat(checkout.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(checkout.getBody().get("total")).isNotNull();
        assertThat((List<?>) checkout.getBody().get("items")).hasSize(1);

        ResponseEntity<List<Map<String, Object>>> orders = restTemplate.exchange(
                url("/api/orders"),
                HttpMethod.GET,
                new HttpEntity<>(auth),
                new ParameterizedTypeReference<>() {
                }
        );
        assertThat(orders.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(orders.getBody()).hasSize(1);

        Integer favoritesCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM favorites f JOIN users u ON u.id = f.user_id WHERE u.login = ?",
                Integer.class,
                login.toLowerCase()
        );
        assertThat(favoritesCount).isEqualTo(1);
    }

    @Test
    void favoritesRequireAuth() {
        ResponseEntity<String> response = restTemplate.getForEntity(url("/api/favorites"), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void homePageHasNoTechnicalTokenCopy() {
        ResponseEntity<String> response = restTemplate.getForEntity(url("/"), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).doesNotContain("Токен");
        assertThat(response.getBody()).doesNotContain("JWT");
        assertThat(response.getBody()).doesNotContain("expiresInMs");
    }

    @Test
    void catalogHomeShowsProfileAndAuthChrome() {
        ResponseEntity<String> page = restTemplate.getForEntity(url("/"), String.class);
        assertThat(page.getBody()).contains("id=\"profile-panel\"");
        assertThat(page.getBody()).contains("id=\"auth-panel\"");
        assertThat(page.getBody()).contains("id=\"auth-form\"");
        assertThat(page.getBody()).contains("id=\"sort-field\"");
        assertThat(page.getBody()).contains("id=\"orders-title\"");
        assertThat(page.getBody()).contains("id=\"favorites-title\"");
        assertThat(page.getBody()).contains("/js/catalog.js");

        ResponseEntity<String> script = restTemplate.getForEntity(url("/js/catalog.js"), String.class);
        assertThat(script.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(script.getBody()).contains("qty-input");
        assertThat(script.getBody()).contains("quantity");
        assertThat(script.getBody()).contains("/api/auth/login");
        assertThat(script.getBody()).contains("/api/auth/register");
        assertThat(script.getBody()).contains("function syncFavoriteButtons");
        assertThat(script.getBody()).contains("function clearProfileUi");
        assertThat(script.getBody()).contains("syncFavoriteButtons();");
    }

    @Test
    void hiddenAttributeIsForcedInCss() {
        ResponseEntity<String> css = restTemplate.getForEntity(url("/css/auth.css"), String.class);
        assertThat(css.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(css.getBody()).contains("[hidden]");
        assertThat(css.getBody()).contains("display: none !important");
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

    private HttpHeaders cartHeaders(String cartId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Cart-Id", cartId);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
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
        HttpHeaders headers = cartHeaders(cartId);
        headers.setBearerAuth(token);
        return headers;
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
