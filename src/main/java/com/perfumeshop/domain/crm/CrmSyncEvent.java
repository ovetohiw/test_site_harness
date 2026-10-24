package com.perfumeshop.domain.crm;

import com.perfumeshop.crm.CrmEventType;
import com.perfumeshop.crm.CrmProvider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "crm_sync_events")
public class CrmSyncEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CrmProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 64)
    private CrmEventType eventType;

    @Column(name = "user_login", nullable = false, length = 64)
    private String userLogin;

    @Column(name = "reference_id", length = 64)
    private String referenceId;

    @Column(name = "external_id", length = 128)
    private String externalId;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(length = 512)
    private String detail;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected CrmSyncEvent() {
    }

    public CrmSyncEvent(
            CrmProvider provider,
            CrmEventType eventType,
            String userLogin,
            String referenceId,
            String externalId,
            String status,
            String detail
    ) {
        this.provider = provider;
        this.eventType = eventType;
        this.userLogin = userLogin;
        this.referenceId = referenceId;
        this.externalId = externalId;
        this.status = status;
        this.detail = truncate(detail);
    }

    private static String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 512 ? value : value.substring(0, 512);
    }

    public Long getId() {
        return id;
    }

    public CrmProvider getProvider() {
        return provider;
    }

    public CrmEventType getEventType() {
        return eventType;
    }

    public String getUserLogin() {
        return userLogin;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getStatus() {
        return status;
    }

    public String getDetail() {
        return detail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
