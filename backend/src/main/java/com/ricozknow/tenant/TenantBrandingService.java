package com.ricozknow.tenant;

import com.ricozknow.audit.AuditService;
import com.ricozknow.common.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TenantBrandingService {

    private final TenantRepository tenantRepository;
    private final AuditService auditService;

    @Transactional
    public Tenant update(String portalTitle, String logoUrl, String primaryColor) {
        Tenant tenant = tenantRepository.findById(TenantContext.get())
                .orElseThrow(() -> new IllegalStateException("Tenant not found"));

        tenant.setPortalTitle(portalTitle);
        tenant.setLogoUrl(logoUrl);
        tenant.setPrimaryColor(primaryColor);

        auditService.record("Tenant", tenant.getId(), "BRANDING_UPDATED", null, portalTitle);
        return tenant;
    }
}
