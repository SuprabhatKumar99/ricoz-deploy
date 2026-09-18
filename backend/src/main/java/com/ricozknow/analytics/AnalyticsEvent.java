package com.ricozknow.analytics;

import com.ricozknow.common.TenantContext;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Deliberately does NOT extend TenantOwnedEntity/TenantOwnedImmutableEntity:
 * this table has neither created_at nor updated_at, only occurred_at
 * (the moment the tracked behavior happened, which for retried/replayed
 * writes may differ from insert time).
 */
@Entity
@Table(name = "analytics_events")
@Getter
@Setter
@NoArgsConstructor
public class AnalyticsEvent {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private AnalyticsEventType eventType;

    @Column(name = "anonymous_session_id")
    private String anonymousSessionId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "article_id")
    private UUID articleId;

    @Column(name = "article_version_id")
    private UUID articleVersionId;

    @JdbcTypeCode(SqlTypes.JSON)
    private String metadata;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt = Instant.now();

    @PrePersist
    protected void assignTenantFromContext() {
        if (this.tenantId == null) {
            this.tenantId = TenantContext.get();
        }
    }
}
