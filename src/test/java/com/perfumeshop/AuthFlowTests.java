package com.perfumeshop;

import com.perfumeshop.service.JwtService;
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
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthFlowTests {

    @LocalServerPort
    private int port;

    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        // Plain client + JDK HttpClient: SimpleClientHttpRequestFactory fails on 401 responses.
        restTemplate = new TestRestTemplate();
        restTemplate.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
    }

    @Test
    void usersTableExists() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables "
                        + "WHERE table_schema = 'public' AND table_name = 'users'",
                Integer.class
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    void registerLoginAndMePersistAcrossRequests() {
        String login = "user_" + UUID.randomUUID().toString().substring(0, 8);
        String password = "secret12";

        ResponseEntity<Map> registerResponse = restTemplate.postForEntity(
                url("/api/auth/register"),
                Map.of("login", login, "password", password),
                Map.class
        );
        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(registerResponse.getBody()).isNotNull();
        assertThat(registerResponse.getBody().get("token")).isInstanceOf(String.class);
        assertThat(((Number) registerResponse.getBody().get("expiresInMs")).longValue())
                .isEqualTo(3_600_000L);
        assertThat(registerResponse.getBody().get("login")).isEqualTo(login.toLowerCase());

        ResponseEntity<Map> loginResponse = restTemplate.postForEntity(
                url("/api/auth/login"),
                Map.of("login", login, "password", password),
                Map.class
        );
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String token = (String) loginResponse.getBody().get("token");
        assertThat(jwtService.isValid(token)).isTrue();
        assertThat(jwtService.extractLogin(token)).isEqualTo(login.toLowerCase());

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<Map> meResponse = restTemplate.exchange(
                url("/api/auth/me"),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
        );
        assertThat(meResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(meResponse.getBody().get("login")).isEqualTo(login.toLowerCase());
    }

    @Test
    void loginRejectsBadPassword() {
        String login = "user_" + UUID.randomUUID().toString().substring(0, 8);
        restTemplate.postForEntity(
                url("/api/auth/register"),
                Map.of("login", login, "password", "secret12"),
                Map.class
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(
                url("/api/auth/login"),
                Map.of("login", login, "password", "wrong-password"),
                Map.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().get("message")).isEqualTo("Invalid credentials");
    }

    @Test
    void duplicateRegisterConflicts() {
        String login = "user_" + UUID.randomUUID().toString().substring(0, 8);
        restTemplate.postForEntity(
                url("/api/auth/register"),
                Map.of("login", login, "password", "secret12"),
                Map.class
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(
                url("/api/auth/register"),
                Map.of("login", login, "password", "secret12"),
                Map.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void meRequiresToken() {
        ResponseEntity<String> response = restTemplate.getForEntity(url("/api/auth/me"), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void loginPageIsServed() {
        ResponseEntity<String> response = restTemplate.getForEntity(url("/"), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Aether");
        assertThat(response.getHeaders().getContentType().toString()).contains(MediaType.TEXT_HTML_VALUE);
    }

    @Test
    void jwtExpiresInOneHour() {
        assertThat(jwtService.expirationMs()).isEqualTo(3_600_000L);
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
