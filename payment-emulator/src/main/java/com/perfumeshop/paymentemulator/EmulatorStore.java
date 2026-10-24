package com.perfumeshop.paymentemulator;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class EmulatorStore {

    private final AtomicLong sequence = new AtomicLong(1);
    private final CopyOnWriteArrayList<EmulatorEvent> events = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<String, StoredPayment> payments = new ConcurrentHashMap<>();

    public long nextId() {
        return sequence.getAndIncrement();
    }

    public EmulatorEvent record(String method, String path, Object payload) {
        long id = nextId();
        EmulatorEvent event = new EmulatorEvent(id, method, path, toMap(payload), Instant.now());
        events.add(event);
        return event;
    }

    public StoredPayment save(StoredPayment payment) {
        payments.put(payment.getId(), payment);
        return payment;
    }

    public Optional<StoredPayment> find(String id) {
        return Optional.ofNullable(payments.get(id));
    }

    public String newPaymentId() {
        return UUID.randomUUID().toString();
    }

    public List<EmulatorEvent> listEvents() {
        return List.copyOf(events);
    }

    public List<StoredPayment> listPayments() {
        return List.copyOf(payments.values());
    }

    public void clear() {
        events.clear();
        payments.clear();
        sequence.set(1);
    }

    public Map<String, Object> status() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("eventCount", events.size());
        status.put("paymentCount", payments.size());
        status.put("nextId", sequence.get());
        status.put("provider", "YOOKASSA");
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
