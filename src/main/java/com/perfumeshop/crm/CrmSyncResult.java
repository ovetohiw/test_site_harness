package com.perfumeshop.crm;

public record CrmSyncResult(
        CrmProvider provider,
        CrmEventType eventType,
        String externalId,
        String status,
        String detail
) {
    public static CrmSyncResult ok(CrmProvider provider, CrmEventType type, String externalId, String detail) {
        return new CrmSyncResult(provider, type, externalId, "SUCCESS", detail);
    }

    public static CrmSyncResult failed(CrmProvider provider, CrmEventType type, String detail) {
        return new CrmSyncResult(provider, type, null, "FAILED", detail);
    }
}
