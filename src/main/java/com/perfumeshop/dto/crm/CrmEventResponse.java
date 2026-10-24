package com.perfumeshop.dto.crm;

import com.perfumeshop.domain.crm.CrmSyncEvent;

import java.time.Instant;

public record CrmEventResponse(
        Long id,
        String provider,
        String eventType,
        String userLogin,
        String referenceId,
        String externalId,
        String status,
        String detail,
        Instant createdAt
) {
    public static CrmEventResponse from(CrmSyncEvent event) {
        return new CrmEventResponse(
                event.getId(),
                event.getProvider().name(),
                event.getEventType().name(),
                event.getUserLogin(),
                event.getReferenceId(),
                event.getExternalId(),
                event.getStatus(),
                event.getDetail(),
                event.getCreatedAt()
        );
    }
}
