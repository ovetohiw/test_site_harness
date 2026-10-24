package com.perfumeshop.paymentemulator;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/emulator")
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
        return store.status();
    }

    @GetMapping("/payments")
    public List<Map<String, Object>> payments() {
        return store.listPayments().stream().map(StoredPayment::toApiObject).toList();
    }

    @PostMapping("/payments/{id}/succeed")
    public Map<String, Object> succeed(@PathVariable String id) {
        StoredPayment payment = store.find(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
        payment.setStatus("succeeded");
        store.record("POST", "/emulator/payments/" + id + "/succeed", Map.of("id", id));
        return payment.toApiObject();
    }

    @PostMapping("/payments/{id}/cancel")
    public Map<String, Object> cancel(@PathVariable String id) {
        StoredPayment payment = store.find(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
        payment.setStatus("canceled");
        store.record("POST", "/emulator/payments/" + id + "/cancel", Map.of("id", id));
        return payment.toApiObject();
    }

    /**
     * Fake confirmation page target (HTML-ish JSON for smoke).
     */
    @GetMapping("/confirm/{id}")
    public Map<String, Object> confirmPage(@PathVariable String id) {
        StoredPayment payment = store.find(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
        Map<String, Object> page = new LinkedHashMap<>();
        page.put("message", "YooKassa emulator confirmation page");
        page.put("paymentId", id);
        page.put("status", payment.getStatus());
        page.put("hint", "POST /emulator/payments/" + id + "/succeed to mark paid");
        return page;
    }
}
