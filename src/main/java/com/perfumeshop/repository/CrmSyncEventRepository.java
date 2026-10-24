package com.perfumeshop.repository;

import com.perfumeshop.domain.crm.CrmSyncEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CrmSyncEventRepository extends JpaRepository<CrmSyncEvent, Long> {

    List<CrmSyncEvent> findTop50ByOrderByCreatedAtDesc();

    List<CrmSyncEvent> findByUserLoginOrderByCreatedAtDesc(String userLogin);

    long countByProviderAndStatus(com.perfumeshop.crm.CrmProvider provider, String status);
}
