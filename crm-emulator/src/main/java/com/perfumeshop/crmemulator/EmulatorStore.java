package com.perfumeshop.crmemulator;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class EmulatorStore {

    private final AtomicLong sequence = new AtomicLong(1);
    private final CopyOnWriteArrayList<EmulatorEvent> events = new CopyOnWriteArrayList<>();

    public long nextId() {
        return sequence.getAndIncrement();
    }

    public EmulatorEvent record(String provider, String method, String path, Object payload) {
        long id = nextId();
        Map<String, Object> body = toMap(payload);
        EmulatorEvent event = new EmulatorEvent(id, provider, method, path, body, Instant.now());
        events.add(event);
        return event;
    }

    public List<EmulatorEvent> listEvents() {
        return List.copyOf(events);
    }

    public void clear() {
        events.clear();
        sequence.set(1);
    }

    public Map<String, Object> status() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("eventCount", events.size());
        status.put("nextId", sequence.get());
        status.put("providers", List.of("BITRIX24", "AMOCRM"));
        return status;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> toMap(Object payload) {
        if (payload == null) {
            return Map.of();
        }
        if (payload instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            map.forEach((k, v) -> copy.put(String.valueOf(k), v));
            return Collections.unmodifiableMap(copy);
        }
        if (payload instanceof List<?> list) {
            return Map.of("items", new ArrayList<>(list));
        }
        return Map.of("value", payload);
    }
}
