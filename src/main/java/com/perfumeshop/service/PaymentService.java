package com.perfumeshop.service;

import com.perfumeshop.domain.order.ShopOrder;
import com.perfumeshop.domain.payment.Payment;
import com.perfumeshop.dto.payment.PaymentResponse;
import com.perfumeshop.dto.payment.PaymentStatusResponse;
import com.perfumeshop.payment.PaymentStatus;
import com.perfumeshop.payment.YooKassaClient;
import com.perfumeshop.payment.YooKassaPaymentResult;
import com.perfumeshop.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final YooKassaClient yooKassaClient;
    private final CrmService crmService;

    public PaymentService(
            PaymentRepository paymentRepository,
            YooKassaClient yooKassaClient,
            CrmService crmService
    ) {
        this.paymentRepository = paymentRepository;
        this.yooKassaClient = yooKassaClient;
        this.crmService = crmService;
    }

    public PaymentStatusResponse status() {
        return new PaymentStatusResponse(
                yooKassaClient.isEnabled(),
                yooKassaClient.mode(),
                "YOOKASSA"
        );
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> listRecent() {
        return paymentRepository.findTop50ByOrderByCreatedAtDesc().stream()
                .map(PaymentResponse::from)
                .toList();
    }

    @Transactional
    public Payment createForOrder(ShopOrder order) {
        String description = "Заказ #" + order.getId() + " — perfume shop";
        YooKassaPaymentResult result = yooKassaClient.createPayment(
                order.getTotal(),
                description,
                Map.of("orderId", String.valueOf(order.getId()))
        );

        Payment payment = new Payment(
                order,
                result.id(),
                PaymentStatus.PENDING,
                result.amount(),
                result.currency(),
                result.confirmationUrl(),
                result.description()
        );
        Payment saved = paymentRepository.save(payment);
        applyStatus(saved, result.status());
        return saved;
    }

    @Transactional
    public PaymentResponse refreshByExternalId(String externalId) {
        Payment payment = paymentRepository.findByExternalId(externalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
        if (payment.getStatus() == PaymentStatus.SUCCEEDED
                || payment.getStatus() == PaymentStatus.CANCELED) {
            return PaymentResponse.from(payment);
        }
        YooKassaPaymentResult remote = yooKassaClient.getPayment(externalId);
        applyStatus(payment, remote.status());
        if (remote.confirmationUrl() != null) {
            payment.setConfirmationUrl(remote.confirmationUrl());
        }
        return PaymentResponse.from(paymentRepository.save(payment));
    }

    @Transactional
    public void handleWebhook(Map<String, Object> notification) {
        Object event = notification.get("event");
        Object object = notification.get("object");
        if (!(object instanceof Map<?, ?> paymentObj)) {
            log.warn("YooKassa webhook without object: {}", event);
            return;
        }
        @SuppressWarnings("unchecked")
        YooKassaPaymentResult result = YooKassaClient.mapPayment((Map<String, Object>) paymentObj);
        Payment payment = paymentRepository.findByExternalId(result.id()).orElse(null);
        if (payment == null) {
            log.warn("YooKassa webhook for unknown payment {}", result.id());
            return;
        }
        applyStatus(payment, result.status());
        paymentRepository.save(payment);
        log.info("YooKassa webhook applied: {} -> {}", result.id(), result.status());
    }

    private void applyStatus(Payment payment, PaymentStatus newStatus) {
        PaymentStatus previous = payment.getStatus();
        payment.setStatus(newStatus);
        ShopOrder order = payment.getOrder();
        order.setPaymentStatus(newStatus);

        boolean becameSucceeded = newStatus == PaymentStatus.SUCCEEDED
                && previous != PaymentStatus.SUCCEEDED;
        if (becameSucceeded) {
            crmService.onSaleCompleted(order);
        }
    }
}
