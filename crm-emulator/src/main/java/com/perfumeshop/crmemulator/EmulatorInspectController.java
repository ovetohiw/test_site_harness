package com.perfumeshop.crmemulator;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(path = "/emulator", produces = MediaType.APPLICATION_JSON_VALUE)
public class EmulatorInspectController {

    private final EmulatorStore store;

    public EmulatorInspectController(EmulatorStore store) {
        this.store = store;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return store.status();
    }

    @GetMapping("/events")
    public List<EmulatorEvent> events() {
        return store.listEvents();
    }

    @DeleteMapping("/events")
    public Map<String, Object> clear() {
        store.clear();
        return Map.of("cleared", true);
    }
}
