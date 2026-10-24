package com.perfumeshop.repository;

import com.perfumeshop.domain.payment.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByExternalId(String externalId);

    Optional<Payment> findByOrder_Id(Long orderId);

    List<Payment> findTop50ByOrderByCreatedAtDesc();
}
