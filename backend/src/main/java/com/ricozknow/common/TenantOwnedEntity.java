package com.ricozknow.common;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for every tenant-owned entity. Every table that stores tenant data
 * must extend this so tenant_id is never forgotten on a new entity.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class TenantOwnedEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Stamps tenantId from the current TenantContext before insert. */
    @PrePersist
    protected void assignTenantFromContext() {
        if (this.tenantId == null) {
            this.tenantId = TenantContext.get();
        }
    }
}
