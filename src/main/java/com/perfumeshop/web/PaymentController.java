package com.perfumeshop.web;

import com.perfumeshop.dto.payment.PaymentResponse;
import com.perfumeshop.dto.payment.PaymentStatusResponse;
import com.perfumeshop.service.PaymentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/status")
    public PaymentStatusResponse status() {
        return paymentService.status();
    }

    @GetMapping
    public List<PaymentResponse> list() {
        return paymentService.listRecent();
    }

    @PostMapping("/{externalId}/refresh")
    public PaymentResponse refresh(@PathVariable String externalId) {
        return paymentService.refreshByExternalId(externalId);
    }

    /**
     * YooKassa HTTP notifications. Public endpoint (no JWT).
     */
    @PostMapping("/webhook")
    public Map<String, String> webhook(@RequestBody Map<String, Object> notification) {
        paymentService.handleWebhook(notification);
        return Map.of("status", "ok");
    }
}
