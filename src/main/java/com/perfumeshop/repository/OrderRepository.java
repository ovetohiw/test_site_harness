package com.perfumeshop.repository;

import com.perfumeshop.domain.order.ShopOrder;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<ShopOrder, Long> {

    @EntityGraph(attributePaths = "items")
    List<ShopOrder> findByUser_IdOrderByCreatedAtDesc(Long userId);
}
