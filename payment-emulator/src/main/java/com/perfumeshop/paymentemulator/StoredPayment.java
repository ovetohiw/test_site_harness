package com.perfumeshop.paymentemulator;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class StoredPayment {

    private final String id;
    private String status;
    private final Map<String, Object> amount;
    private final String description;
    private final Map<String, Object> metadata;
    private final String confirmationUrl;
    private final Instant createdAt;

    public StoredPayment(
            String id,
            String status,
            Map<String, Object> amount,
            String description,
            Map<String, Object> metadata,
            String confirmationUrl
    ) {
        this.id = id;
        this.status = status;
        this.amount = amount;
        this.description = description;
        this.metadata = metadata;
        this.confirmationUrl = confirmationUrl;
        this.createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Map<String, Object> toApiObject() {
        Map<String, Object> obj = new LinkedHashMap<>();
        obj.put("id", id);
        obj.put("status", status);
        obj.put("paid", "succeeded".equals(status));
        obj.put("amount", amount);
        obj.put("description", description);
        obj.put("metadata", metadata);
        obj.put("created_at", createdAt.toString());
        if (confirmationUrl != null && "pending".equals(status)) {
            Map<String, Object> confirmation = new LinkedHashMap<>();
            confirmation.put("type", "redirect");
            confirmation.put("confirmation_url", confirmationUrl);
            obj.put("confirmation", confirmation);
        }
        return obj;
    }
}
