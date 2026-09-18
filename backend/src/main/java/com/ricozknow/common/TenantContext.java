package com.ricozknow.common;

import java.util.UUID;

/**
 * Thread-local holder for the resolved tenant of the current request.
 * Populated by TenantResolverFilter before the request reaches any controller,
 * and cleared at the end of the request to avoid leaking across thread-pool reuse.
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(UUID tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static UUID get() {
        UUID tenantId = CURRENT_TENANT.get();
        if (tenantId == null) {
            throw new IllegalStateException("No tenant resolved for current request");
        }
        return tenantId;
    }

    public static UUID getOrNull() {
        return CURRENT_TENANT.get();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
