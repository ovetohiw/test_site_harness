package com.perfumeshop.crm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * amoCRM API v4 client (Bearer access token).
 * Endpoints: /api/v4/contacts, /api/v4/leads, /api/v4/events (behavior note via notes).
 */
@Component
public class AmoCrmClient implements CrmClient {

    private static final Logger log = LoggerFactory.getLogger(AmoCrmClient.class);

    private final CrmProperties.AmoCrm props;
    private final RestClient restClient;

    public AmoCrmClient(CrmProperties properties, RestClient.Builder restClientBuilder) {
        this.props = properties.getAmocrm();
        this.restClient = restClientBuilder.build();
    }

    @Override
    public CrmProvider provider() {
        return CrmProvider.AMOCRM;
    }

    @Override
    public boolean isEnabled() {
        return props.isEnabled();
    }

    @Override
    public CrmSyncResult syncCustomer(CrmCustomer customer) {
        if (!props.isLive()) {
            return CrmSyncResult.ok(
                    provider(),
                    CrmEventType.CUSTOMER_REGISTERED,
                    "stub-amo-contact-" + customer.userId(),
                    "stub: contact for " + customer.login()
            );
        }
        try {
            Map<String, Object> contact = new LinkedHashMap<>();
            contact.put("name", customer.login());
            contact.put("custom_fields_values", List.of());

            Map<String, Object> response = postList("/api/v4/contacts", List.of(contact));
            String externalId = firstEmbeddedId(response, "contacts");
            return CrmSyncResult.ok(provider(), CrmEventType.CUSTOMER_REGISTERED, externalId, "POST /api/v4/contacts");
        } catch (RuntimeException ex) {
            log.warn("amoCRM syncCustomer failed: {}", ex.getMessage());
            return CrmSyncResult.failed(provider(), CrmEventType.CUSTOMER_REGISTERED, ex.getMessage());
        }
    }

    @Override
    public CrmSyncResult syncSale(CrmSale sale) {
        if (!props.isLive()) {
            return CrmSyncResult.ok(
                    provider(),
                    CrmEventType.SALE_COMPLETED,
                    "stub-amo-lead-" + sale.orderId(),
                    "stub: lead " + sale.title()
            );
        }
        try {
            Map<String, Object> lead = new LinkedHashMap<>();
            lead.put("name", sale.title());
            lead.put("price", sale.total().intValue());
            lead.put("request_id", String.valueOf(sale.orderId()));

            Map<String, Object> response = postList("/api/v4/leads", List.of(lead));
            String externalId = firstEmbeddedId(response, "leads");
            return CrmSyncResult.ok(provider(), CrmEventType.SALE_COMPLETED, externalId, "POST /api/v4/leads");
        } catch (RuntimeException ex) {
            log.warn("amoCRM syncSale failed: {}", ex.getMessage());
            return CrmSyncResult.failed(provider(), CrmEventType.SALE_COMPLETED, ex.getMessage());
        }
    }

    @Override
    public CrmSyncResult syncBehavior(CrmBehavior behavior) {
        if (!props.isLive()) {
            return CrmSyncResult.ok(
                    provider(),
                    CrmEventType.FAVORITE_ADDED,
                    "stub-amo-behavior-" + UUID.randomUUID(),
                    "stub: favorite " + behavior.productName()
            );
        }
        try {
            // Unsorted note / lead-like signal for interest tracking
            Map<String, Object> note = new LinkedHashMap<>();
            note.put("entity_id", 0);
            note.put("note_type", "common");
            note.put("params", Map.of(
                    "text", "Favorite: " + behavior.productName()
                            + " (productId=" + behavior.productId() + ") by " + behavior.login()
            ));

            Map<String, Object> response = postList("/api/v4/contacts/notes", List.of(note));
            String externalId = firstEmbeddedId(response, "notes");
            if (externalId == null) {
                externalId = "note-ok";
            }
            return CrmSyncResult.ok(provider(), CrmEventType.FAVORITE_ADDED, externalId, "POST /api/v4/contacts/notes");
        } catch (RuntimeException ex) {
            log.warn("amoCRM syncBehavior failed: {}", ex.getMessage());
            return CrmSyncResult.failed(provider(), CrmEventType.FAVORITE_ADDED, ex.getMessage());
        }
    }

    private Map<String, Object> postList(String path, List<Map<String, Object>> body) {
        String base = props.getBaseUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        try {
            Map<String, Object> response = restClient.post()
                    .uri(base + path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + props.getAccessToken())
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            if (response == null) {
                throw new RestClientException("Empty amoCRM response for " + path);
            }
            return response;
        } catch (RestClientException ex) {
            throw ex;
        }
    }

    @SuppressWarnings("unchecked")
    private static String firstEmbeddedId(Map<String, Object> response, String collection) {
        Object embedded = response.get("_embedded");
        if (!(embedded instanceof Map<?, ?> embeddedMap)) {
            return null;
        }
        Object items = embeddedMap.get(collection);
        if (!(items instanceof List<?> list) || list.isEmpty()) {
            return null;
        }
        Object first = list.get(0);
        if (!(first instanceof Map<?, ?> item)) {
            return null;
        }
        Object id = item.get("id");
        return id == null ? null : String.valueOf(id);
    }
}
