package com.ricozknow.common;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for immutable, append-only tenant-owned records (audit_logs,
 * analytics_events-style tables) that never get an updated_at column because
 * they are never updated after insert. Use TenantOwnedEntity instead for
 * anything that gets mutated post-creation.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class TenantOwnedImmutableEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void assignTenantFromContext() {
        if (this.tenantId == null) {
            this.tenantId = TenantContext.get();
        }
    }
}
