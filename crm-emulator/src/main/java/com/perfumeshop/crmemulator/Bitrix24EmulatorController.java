package com.perfumeshop.crmemulator;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Mimics Bitrix24 inbound webhook REST:
 * POST /rest/{userId}/{token}/{method}.json → {"result": &lt;id&gt;}
 */
@RestController
public class Bitrix24EmulatorController {

    private final EmulatorStore store;

    public Bitrix24EmulatorController(EmulatorStore store) {
        this.store = store;
    }

    @PostMapping(
            value = "/rest/{userId}/{token}/{method}.json",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Map<String, Object> invoke(
            @PathVariable String userId,
            @PathVariable String token,
            @PathVariable String method,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        String path = "/rest/" + userId + "/" + token + "/" + method + ".json";
        EmulatorEvent event = store.record("BITRIX24", method, path, body == null ? Map.of() : body);
        return Map.of("result", event.id());
    }
}
