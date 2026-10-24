package com.perfumeshop.crm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Bitrix24 inbound-webhook client.
 * Methods: crm.contact.add, crm.deal.add, crm.timeline.comment.add (behavior).
 */
@Component
public class Bitrix24CrmClient implements CrmClient {

    private static final Logger log = LoggerFactory.getLogger(Bitrix24CrmClient.class);

    private final CrmProperties.Bitrix24 props;
    private final RestClient restClient;

    public Bitrix24CrmClient(CrmProperties properties, RestClient.Builder restClientBuilder) {
        this.props = properties.getBitrix24();
        this.restClient = restClientBuilder.build();
    }

    @Override
    public CrmProvider provider() {
        return CrmProvider.BITRIX24;
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
                    "stub-bitrix-contact-" + customer.userId(),
                    "stub: contact for " + customer.login()
            );
        }
        try {
            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put("NAME", customer.login());
            fields.put("OPENED", "Y");
            fields.put("TYPE_ID", "CLIENT");
            fields.put("SOURCE_ID", "WEB");
            fields.put("COMMENTS", "Registered in perfume shop, userId=" + customer.userId());

            Map<?, ?> response = post("crm.contact.add", Map.of("fields", fields));
            Object result = response.get("result");
            String externalId = result == null ? null : String.valueOf(result);
            return CrmSyncResult.ok(provider(), CrmEventType.CUSTOMER_REGISTERED, externalId, "crm.contact.add");
        } catch (RuntimeException ex) {
            log.warn("Bitrix24 syncCustomer failed: {}", ex.getMessage());
            return CrmSyncResult.failed(provider(), CrmEventType.CUSTOMER_REGISTERED, ex.getMessage());
        }
    }

    @Override
    public CrmSyncResult syncSale(CrmSale sale) {
        if (!props.isLive()) {
            return CrmSyncResult.ok(
                    provider(),
                    CrmEventType.SALE_COMPLETED,
                    "stub-bitrix-deal-" + sale.orderId(),
                    "stub: deal " + sale.title()
            );
        }
        try {
            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put("TITLE", sale.title());
            fields.put("OPPORTUNITY", sale.total());
            fields.put("CURRENCY_ID", "RUB");
            fields.put("COMMENTS", sale.description());
            fields.put("SOURCE_ID", "WEB");

            Map<?, ?> response = post("crm.deal.add", Map.of("fields", fields));
            Object result = response.get("result");
            String externalId = result == null ? null : String.valueOf(result);
            return CrmSyncResult.ok(provider(), CrmEventType.SALE_COMPLETED, externalId, "crm.deal.add");
        } catch (RuntimeException ex) {
            log.warn("Bitrix24 syncSale failed: {}", ex.getMessage());
            return CrmSyncResult.failed(provider(), CrmEventType.SALE_COMPLETED, ex.getMessage());
        }
    }

    @Override
    public CrmSyncResult syncBehavior(CrmBehavior behavior) {
        if (!props.isLive()) {
            return CrmSyncResult.ok(
                    provider(),
                    CrmEventType.FAVORITE_ADDED,
                    "stub-bitrix-behavior-" + UUID.randomUUID(),
                    "stub: favorite " + behavior.productName()
            );
        }
        try {
            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put("ENTITY_ID", "contact");
            fields.put("ENTITY_TYPE", "contact");
            fields.put("COMMENT", "Favorite: " + behavior.productName()
                    + " (productId=" + behavior.productId() + ") by " + behavior.login());

            Map<?, ?> response = post("crm.timeline.comment.add", Map.of("fields", fields));
            Object result = response.get("result");
            String externalId = result == null ? "ok" : String.valueOf(result);
            return CrmSyncResult.ok(provider(), CrmEventType.FAVORITE_ADDED, externalId, "crm.timeline.comment.add");
        } catch (RuntimeException ex) {
            log.warn("Bitrix24 syncBehavior failed: {}", ex.getMessage());
            return CrmSyncResult.failed(provider(), CrmEventType.FAVORITE_ADDED, ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<?, ?> post(String method, Map<String, Object> body) {
        String base = props.getWebhookBaseUrl();
        if (!base.endsWith("/")) {
            base = base + "/";
        }
        try {
            Map<?, ?> response = restClient.post()
                    .uri(base + method + ".json")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            if (response == null) {
                throw new RestClientException("Empty Bitrix24 response for " + method);
            }
            if (response.containsKey("error")) {
                throw new RestClientException(String.valueOf(response.get("error_description")));
            }
            return response;
        } catch (RestClientException ex) {
            throw ex;
        }
    }
}
