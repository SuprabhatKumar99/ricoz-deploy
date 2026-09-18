package com.ricozknow.tenant;

import com.ricozknow.audit.AuditService;
import com.ricozknow.common.TenantContext;
import com.ricozknow.role.Role;
import com.ricozknow.role.RoleName;
import com.ricozknow.role.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TenantProvisioningService {

    private final TenantRepository tenantRepository;
    private final RoleRepository roleRepository;
    private final AuditService auditService;

    /**
     * Creates a new tenant and seeds its fixed MVP role set (ADMIN, EDITOR, REVIEWER,
     * AGENT_VIEWER). Called from an internal/ops-only endpoint or a signup flow —
     * intentionally not exposed as self-serve tenant creation in MVP.
     */
    @Transactional
    public Tenant provision(String name, String slug, String subdomain) {
        Tenant tenant = new Tenant();
        tenant.setName(name);
        tenant.setSlug(slug);
        tenant.setSubdomain(subdomain);
        tenant.setStatus(Tenant.TenantStatus.ACTIVE);
        tenant = tenantRepository.save(tenant);

        // Role seeding happens inside the new tenant's context so TenantOwnedEntity
        // can stamp tenant_id via @PrePersist.
        TenantContext.set(tenant.getId());
        try {
            for (RoleName roleName : RoleName.values()) {
                roleRepository.save(new Role(roleName));
            }
            auditService.record("Tenant", tenant.getId(), "TENANT_PROVISIONED", null, tenant);
        } finally {
            TenantContext.clear();
        }

        return tenant;
    }
}
