package com.perfumeshop.crmemulator;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * Mimics amoCRM API v4 endpoints used by the shop client.
 */
@RestController
public class AmoCrmEmulatorController {

    private final EmulatorStore store;
    private final EmulatorProperties properties;

    public AmoCrmEmulatorController(EmulatorStore store, EmulatorProperties properties) {
        this.store = store;
        this.properties = properties;
    }

    @PostMapping(
            value = "/api/v4/contacts",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Map<String, Object> createContacts(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody List<Map<String, Object>> body
    ) {
        requireBearer(authorization);
        EmulatorEvent event = store.record("AMOCRM", "contacts", "/api/v4/contacts", body);
        return embedded("contacts", event.id());
    }

    @PostMapping(
            value = "/api/v4/leads",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Map<String, Object> createLeads(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody List<Map<String, Object>> body
    ) {
        requireBearer(authorization);
        EmulatorEvent event = store.record("AMOCRM", "leads", "/api/v4/leads", body);
        return embedded("leads", event.id());
    }

    @PostMapping(
            value = "/api/v4/contacts/notes",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Map<String, Object> createContactNotes(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody List<Map<String, Object>> body
    ) {
        requireBearer(authorization);
        EmulatorEvent event = store.record("AMOCRM", "contacts/notes", "/api/v4/contacts/notes", body);
        return embedded("notes", event.id());
    }

    private void requireBearer(String authorization) {
        String expected = "Bearer " + properties.getAmocrmAccessToken();
        if (authorization == null || !authorization.equals(expected)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or missing Bearer token");
        }
    }

    private static Map<String, Object> embedded(String collection, long id) {
        return Map.of("_embedded", Map.of(collection, List.of(Map.of("id", id))));
    }
}
