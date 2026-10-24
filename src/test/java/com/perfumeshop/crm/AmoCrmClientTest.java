package com.perfumeshop.crm;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class AmoCrmClientTest {

    private HttpServer server;
    private final AtomicReference<String> lastPath = new AtomicReference<>();
    private final AtomicReference<String> lastAuth = new AtomicReference<>();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", exchange -> {
            lastPath.set(exchange.getRequestURI().getPath());
            lastAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] response = ("{\"_embedded\":{\"contacts\":[{\"id\":15}],\"leads\":[{\"id\":88}]}}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void liveModePostsContactAndLeadWithBearerToken() {
        String base = "http://localhost:" + server.getAddress().getPort();
        CrmProperties properties = new CrmProperties();
        properties.getAmocrm().setEnabled(true);
        properties.getAmocrm().setBaseUrl(base);
        properties.getAmocrm().setAccessToken("test-token");

        AmoCrmClient client = new AmoCrmClient(properties, RestClient.builder());

        CrmSyncResult contact = client.syncCustomer(new CrmCustomer(3L, "bob"));
        assertThat(contact.status()).isEqualTo("SUCCESS");
        assertThat(contact.externalId()).isEqualTo("15");
        assertThat(lastPath.get()).isEqualTo("/api/v4/contacts");
        assertThat(lastAuth.get()).isEqualTo("Bearer test-token");

        CrmSyncResult lead = client.syncSale(new CrmSale(
                5L,
                "bob",
                java.math.BigDecimal.valueOf(1200),
                java.util.List.of(new CrmSale.Line("Oud", 2, java.math.BigDecimal.valueOf(600)))
        ));
        assertThat(lead.status()).isEqualTo("SUCCESS");
        assertThat(lead.externalId()).isEqualTo("88");
        assertThat(lastPath.get()).isEqualTo("/api/v4/leads");
    }
}
