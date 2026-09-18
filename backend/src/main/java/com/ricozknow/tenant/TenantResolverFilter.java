// package com.ricozknow.tenant;

// import com.ricozknow.common.TenantContext;
// import com.ricozknow.user.AuthenticatedUser;
// import jakarta.servlet.FilterChain;
// import jakarta.servlet.ServletException;
// import jakarta.servlet.http.HttpServletRequest;
// import jakarta.servlet.http.HttpServletResponse;
// import lombok.RequiredArgsConstructor;
// import org.slf4j.MDC;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.security.core.Authentication;
// import org.springframework.security.core.context.SecurityContextHolder;
// import org.springframework.stereotype.Component;
// import org.springframework.web.filter.OncePerRequestFilter;

// import java.io.IOException;
// import java.util.Optional;
// import java.util.UUID;

// /**
//  * Resolves the tenant for every request and stores it in TenantContext for the
//  * lifetime of the request.
//  *
//  * Resolution order:
//  *   1. Authenticated principal's tenantId (agent/knowledge-studio/admin APIs) — this
//  *      is authoritative and cannot be overridden by request headers/hostname.
//  *   2. Hostname subdomain (customer portal, unauthenticated requests), e.g.
//  *      acme.ricozknow.com -> "acme".
//  *
//  * This mirrors the defense-in-depth model in the spec: Angular resolves tenant from
//  * hostname for UX purposes, but Spring Boot independently re-resolves and validates it.
//  * A request is rejected (400) if no tenant can be resolved.
//  */
// @Component
// @RequiredArgsConstructor
// public class TenantResolverFilter extends OncePerRequestFilter {

//     private final TenantRepository tenantRepository;

//     @Value("${ricozknow.tenant.base-domain}")
//     private String baseDomain;

//     @Override
//     protected void doFilterInternal(HttpServletRequest request,
//                                      HttpServletResponse response,
//                                      FilterChain filterChain) throws ServletException, IOException {
//         try {
//             Optional<UUID> tenantId = resolveFromAuthenticatedPrincipal()
//                     .or(() -> resolveFromHostname(request));

//             if (tenantId.isEmpty()) {
//                 response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unable to resolve tenant");
//                 return;
//             }

//             TenantContext.set(tenantId.get());
//             MDC.put("tenantId", tenantId.get().toString());
//             filterChain.doFilter(request, response);
//         } finally {
//             TenantContext.clear();
//             MDC.remove("tenantId");
//         }
//     }

//     private Optional<UUID> resolveFromAuthenticatedPrincipal() {
//         Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//         if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser principal) {
//             return Optional.of(principal.tenantId());
//         }
//         return Optional.empty();
//     }

//     private Optional<UUID> resolveFromHostname(HttpServletRequest request) {
//         String host = request.getServerName();
//         if (host == null || !host.endsWith("." + baseDomain)) {
//             return Optional.empty();
//         }
//         String subdomain = host.substring(0, host.length() - baseDomain.length() - 1);
//         return tenantRepository.findBySubdomain(subdomain).map(Tenant::getId);
//     }

//     @Override
//     protected boolean shouldNotFilter(HttpServletRequest request) {
//         String path = request.getRequestURI();
//         // Public, tenant-agnostic endpoints (health checks, docs, auth entry points
//         // that themselves determine tenant from the login payload).
//         return path.startsWith("/actuator")
//                 || path.startsWith("/swagger-ui")
//                 || path.startsWith("/v3/api-docs")
//                 || path.equals("/api/v1/auth/login")
//                 || path.equals("/api/v1/auth/refresh")
//                 || path.equals("/api/v1/auth/logout")
//                 || path.equals("/api/v1/auth/register")
//                 || path.equals("/api/v1/tenants/lookup");
//     }
// }



package com.ricozknow.tenant;

import com.ricozknow.common.TenantContext;
import com.ricozknow.user.AuthenticatedUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/**
 * Resolves the tenant for every request and stores it in TenantContext for the
 * lifetime of the request.
 *
 * Resolution order:
 *
 *   1. Authenticated principal's tenantId.
 *      This is authoritative for authenticated APIs and cannot be overridden
 *      by a query parameter or hostname.
 *
 *   2. Public portal tenant query parameter.
 *      Example:
 *
 *          /api/v1/portal/articles?tenant=testorg
 *
 *      The parameter is treated as a tenant subdomain/slug and is resolved
 *      against the database. Only ACTIVE tenants are accepted.
 *
 *   3. Hostname subdomain.
 *      Example:
 *
 *          testorg.ricozknow.com
 *
 *      resolves to the "testorg" tenant.
 *
 * A request is rejected with 400 when no tenant can be resolved.
 */
@Component
@RequiredArgsConstructor
public class TenantResolverFilter extends OncePerRequestFilter {

    private final TenantRepository tenantRepository;

    @Value("${ricozknow.tenant.base-domain}")
    private String baseDomain;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            Optional<UUID> tenantId = resolveFromAuthenticatedPrincipal()
                    .or(() -> resolveFromPortalParameter(request))
                    .or(() -> resolveFromHostname(request));

            if (tenantId.isEmpty()) {
                response.sendError(
                        HttpServletResponse.SC_BAD_REQUEST,
                        "Unable to resolve tenant"
                );
                return;
            }

            TenantContext.set(tenantId.get());
            MDC.put("tenantId", tenantId.get().toString());

            filterChain.doFilter(request, response);

        } finally {
            TenantContext.clear();
            MDC.remove("tenantId");
        }
    }

    /**
     * Authenticated users always use the tenant stored in their principal.
     *
     * This prevents an authenticated user from changing tenant context by
     * sending ?tenant=another-tenant.
     */
    private Optional<UUID> resolveFromAuthenticatedPrincipal() {

        Authentication auth =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (auth != null
                && auth.getPrincipal() instanceof AuthenticatedUser principal) {

            return Optional.of(principal.tenantId());
        }

        return Optional.empty();
    }

    /**
     * Resolves the tenant for anonymous customer-portal requests.
     *
     * Example:
     *
     *     GET /api/v1/portal/articles?tenant=testorg
     *
     * The value is resolved using the tenant's subdomain field and only
     * ACTIVE tenants are accepted.
     */
    private Optional<UUID> resolveFromPortalParameter(
            HttpServletRequest request
    ) {

        if (!request.getRequestURI().startsWith("/api/v1/portal/")) {
            return Optional.empty();
        }

        String tenant = request.getParameter("tenant");

        if (tenant == null || tenant.isBlank()) {
            return Optional.empty();
        }

        String subdomain = tenant.trim().toLowerCase();

        return tenantRepository
                .findBySubdomain(subdomain)
                .filter(t -> t.getStatus() == Tenant.TenantStatus.ACTIVE)
                .map(Tenant::getId);
    }

    /**
     * Production portal resolution.
     *
     * Example:
     *
     *     testorg.ricozknow.com
     *
     * becomes:
     *
     *     testorg
     */
    private Optional<UUID> resolveFromHostname(
            HttpServletRequest request
    ) {

        String host = request.getServerName();

        if (host == null || !host.endsWith("." + baseDomain)) {
            return Optional.empty();
        }

        String subdomain =
                host.substring(
                        0,
                        host.length() - baseDomain.length() - 1
                );

        return tenantRepository
                .findBySubdomain(subdomain)
                .filter(t -> t.getStatus() == Tenant.TenantStatus.ACTIVE)
                .map(Tenant::getId);
    }

    @Override
    protected boolean shouldNotFilter(
            HttpServletRequest request
    ) {

        String path = request.getRequestURI();

        return path.startsWith("/actuator")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")

                // Tenant-independent authentication endpoints
                || path.equals("/api/v1/auth/login")
                || path.equals("/api/v1/auth/register")
                || path.equals("/api/v1/auth/refresh")
                || path.equals("/api/v1/auth/logout")

                // Tenant lookup resolves its own tenant
                || path.equals("/api/v1/tenants/lookup");
    }
}