package com.perfumeshop.crmemulator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the same HTTP shapes as perfume-shop Bitrix24CrmClient / AmoCrmClient.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ShopClientContractTests {

    @LocalServerPort
    private int port;

    @Autowired
    private EmulatorStore store;

    private RestClient restClient;

    @BeforeEach
    void setUp() {
        store.clear();
        restClient = RestClient.builder().build();
    }

    @Test
    void bitrix24ClientShapeSucceeds() {
        String base = "http://localhost:" + port + "/rest/1/test/";

        @SuppressWarnings("unchecked")
        Map<String, Object> contact = restClient.post()
                .uri(base + "crm.contact.add.json")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("fields", Map.of("NAME", "alice", "OPENED", "Y")))
                .retrieve()
                .body(Map.class);
        assertThat(contact.get("result")).isEqualTo(1);

        @SuppressWarnings("unchecked")
        Map<String, Object> deal = restClient.post()
                .uri(base + "crm.deal.add.json")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("fields", Map.of("TITLE", "Order #99", "OPPORTUNITY", 10)))
                .retrieve()
                .body(Map.class);
        assertThat(deal.get("result")).isEqualTo(2);

        @SuppressWarnings("unchecked")
        Map<String, Object> comment = restClient.post()
                .uri(base + "crm.timeline.comment.add.json")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("fields", Map.of("COMMENT", "Favorite: Rose")))
                .retrieve()
                .body(Map.class);
        assertThat(comment.get("result")).isEqualTo(3);

        assertThat(store.listEvents()).hasSize(3);
    }

    @Test
    void amoCrmClientShapeSucceeds() {
        String base = "http://localhost:" + port;

        Map<String, Object> contactBody = new LinkedHashMap<>();
        contactBody.put("name", "bob");
        Map<String, Object> contactResponse = restClient.post()
                .uri(base + "/api/v4/contacts")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer test-token")
                .body(List.of(contactBody))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        assertThat(firstEmbeddedId(contactResponse, "contacts")).isEqualTo("1");

        Map<String, Object> leadBody = new LinkedHashMap<>();
        leadBody.put("name", "Order #5");
        leadBody.put("price", 1200);
        Map<String, Object> leadResponse = restClient.post()
                .uri(base + "/api/v4/leads")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer test-token")
                .body(List.of(leadBody))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        assertThat(firstEmbeddedId(leadResponse, "leads")).isEqualTo("2");

        Map<String, Object> noteBody = new LinkedHashMap<>();
        noteBody.put("entity_id", 0);
        noteBody.put("note_type", "common");
        noteBody.put("params", Map.of("text", "Favorite: Oud"));
        Map<String, Object> noteResponse = restClient.post()
                .uri(base + "/api/v4/contacts/notes")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer test-token")
                .body(List.of(noteBody))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        assertThat(firstEmbeddedId(noteResponse, "notes")).isEqualTo("3");
    }

    @SuppressWarnings("unchecked")
    private static String firstEmbeddedId(Map<String, Object> response, String collection) {
        Map<String, Object> embedded = (Map<String, Object>) response.get("_embedded");
        List<Map<String, Object>> items = (List<Map<String, Object>>) embedded.get(collection);
        return String.valueOf(items.get(0).get("id"));
    }
}
