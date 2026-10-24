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
class CatalogCartTests {

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
    void productsTableExistsAndSeeded() {
        Integer tables = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables "
                        + "WHERE table_schema = 'public' AND table_name = 'products'",
                Integer.class
        );
        assertThat(tables).isEqualTo(1);

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM products", Integer.class);
        assertThat(count).isGreaterThanOrEqualTo(4);
    }

    @Test
    void anonymousCanListProductsWithRequiredFields() {
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url("/api/products"),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                }
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull().isNotEmpty();

        Map<String, Object> product = response.getBody().get(0);
        assertThat(product).containsKeys("id", "name", "brand", "imageUrl", "price", "description");
        assertThat(product.get("name")).isInstanceOf(String.class);
        assertThat(product.get("brand")).asString().isNotBlank();
        assertThat(product.get("imageUrl")).asString().startsWith("/images/");
        assertThat(product.get("description")).asString().isNotBlank();
        assertThat(((Number) product.get("price")).doubleValue()).isPositive();
    }

    @Test
    void anonymousCanAddAndRemoveCartItems() {
        String cartId = UUID.randomUUID().toString();

        ResponseEntity<List<Map<String, Object>>> productsResponse = restTemplate.exchange(
                url("/api/products"),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                }
        );
        Long productId = ((Number) productsResponse.getBody().get(0).get("id")).longValue();

        HttpHeaders headers = new HttpHeaders();
        headers.set(CartHeader.NAME, cartId);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> addResponse = restTemplate.exchange(
                url("/api/cart/items"),
                HttpMethod.POST,
                new HttpEntity<>(Map.of("productId", productId, "quantity", 1), headers),
                Map.class
        );
        assertThat(addResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(addResponse.getBody()).isNotNull();
        assertThat((List<?>) addResponse.getBody().get("items")).hasSize(1);
        assertThat(addResponse.getBody().get("cartId")).isEqualTo(cartId);

        ResponseEntity<Map> getResponse = restTemplate.exchange(
                url("/api/cart"),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
        );
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((List<?>) getResponse.getBody().get("items")).hasSize(1);

        ResponseEntity<Map> removeResponse = restTemplate.exchange(
                url("/api/cart/items/" + productId),
                HttpMethod.DELETE,
                new HttpEntity<>(headers),
                Map.class
        );
        assertThat(removeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((List<?>) removeResponse.getBody().get("items")).isEmpty();
    }

    @Test
    void catalogPageIsPublic() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity(url("/"), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Aether");
        assertThat(response.getBody()).contains("id=\"cart-panel\"");
        assertThat(response.getBody()).contains("id=\"product-grid\"");
        assertThat(response.getBody()).contains("id=\"sort-field\"");
        assertThat(response.getBody()).contains("id=\"profile-panel\"");
        assertThat(response.getHeaders().getContentType().toString()).contains(MediaType.TEXT_HTML_VALUE);

        assertRedirectsToHome("/catalog.html");
        assertRedirectsToHome("/index.html");
    }

    private void assertRedirectsToHome(String path) throws Exception {
        java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                .followRedirects(java.net.http.HttpClient.Redirect.NEVER)
                .build();
        java.net.http.HttpResponse<Void> response = client.send(
                java.net.http.HttpRequest.newBuilder(java.net.URI.create(url(path))).GET().build(),
                java.net.http.HttpResponse.BodyHandlers.discarding()
        );
        assertThat(response.statusCode()).isEqualTo(HttpStatus.FOUND.value());
        String location = response.headers().firstValue("Location").orElse("");
        assertThat(location).endsWith("/").doesNotContain(".html");
    }

    @Test
    void addUnknownProductReturnsNotFound() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(CartHeader.NAME, UUID.randomUUID().toString());
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/cart/items"),
                HttpMethod.POST,
                new HttpEntity<>(Map.of("productId", 999_999L, "quantity", 1), headers),
                Map.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private static final class CartHeader {
        static final String NAME = "X-Cart-Id";
    }
}
