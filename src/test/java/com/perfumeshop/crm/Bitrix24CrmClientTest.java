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

class Bitrix24CrmClientTest {

    private HttpServer server;
    private final AtomicReference<String> lastPath = new AtomicReference<>();
    private final AtomicReference<String> lastBody = new AtomicReference<>();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", exchange -> {
            lastPath.set(exchange.getRequestURI().getPath());
            lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = "{\"result\":42}".getBytes(StandardCharsets.UTF_8);
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
    void liveModePostsContactAndDealToWebhook() {
        String base = "http://localhost:" + server.getAddress().getPort() + "/rest/1/secret/";
        CrmProperties properties = new CrmProperties();
        properties.getBitrix24().setEnabled(true);
        properties.getBitrix24().setWebhookBaseUrl(base);

        Bitrix24CrmClient client = new Bitrix24CrmClient(properties, RestClient.builder());

        CrmSyncResult contact = client.syncCustomer(new CrmCustomer(7L, "alice"));
        assertThat(contact.status()).isEqualTo("SUCCESS");
        assertThat(contact.externalId()).isEqualTo("42");
        assertThat(lastPath.get()).contains("crm.contact.add");
        assertThat(lastBody.get()).contains("alice");

        CrmSyncResult deal = client.syncSale(new CrmSale(
                99L,
                "alice",
                java.math.BigDecimal.TEN,
                java.util.List.of(new CrmSale.Line("Rose", 1, java.math.BigDecimal.TEN))
        ));
        assertThat(deal.status()).isEqualTo("SUCCESS");
        assertThat(lastPath.get()).contains("crm.deal.add");
        assertThat(lastBody.get()).contains("Order #99");
    }
}
