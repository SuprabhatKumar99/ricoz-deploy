package com.ricozknow.tenant;

import com.ricozknow.common.TenantContext;
import com.ricozknow.tenant.dto.TenantLookupResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class TenantController {

    private final TenantRepository tenantRepository;
    private final TenantBrandingService brandingService;

    /**
     * Public, tenant-agnostic lookup used by the portal shell before any tenant
     * context exists (it's how the SPA turns a hostname into a slug + branding
     * to render the login screen / header). Never returns anything sensitive.
     */
    @GetMapping("/api/v1/tenants/lookup")
    public TenantLookupResponse lookup(@RequestParam String subdomain) {
        Tenant tenant = tenantRepository.findBySubdomain(subdomain)
                .filter(t -> t.getStatus() == Tenant.TenantStatus.ACTIVE)
                .orElseThrow(() -> new IllegalStateException("Unknown or inactive tenant"));
        return TenantLookupResponse.from(tenant);
    }

     /**
     * Returns the tenant belonging to the currently authenticated user.
     *
     * TenantResolverFilter resolves the authenticated user's tenantId from
     * AuthenticatedUser and stores it in TenantContext before this controller
     * is invoked.
     */
    @GetMapping("/api/v1/tenants/me")
    @PreAuthorize("isAuthenticated()")
    public TenantLookupResponse currentTenant() {

        Tenant tenant = tenantRepository.findById(TenantContext.get())
                .filter(t -> t.getStatus() == Tenant.TenantStatus.ACTIVE)
                .orElseThrow(() -> new IllegalStateException("Tenant not found"));

        return TenantLookupResponse.from(tenant);
    }


    @PutMapping("/api/v1/admin/branding")
    @PreAuthorize("hasRole('ADMIN')")
    public TenantLookupResponse updateBranding(@RequestBody BrandingRequest request) {
        return TenantLookupResponse.from(brandingService.update(request.portalTitle(), request.logoUrl(), request.primaryColor()));
    }

    public record BrandingRequest(String portalTitle, String logoUrl, String primaryColor) {
    }
}
