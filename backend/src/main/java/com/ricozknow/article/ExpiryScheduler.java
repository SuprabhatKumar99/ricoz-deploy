package com.ricozknow.article;

import com.ricozknow.audit.AuditService;
import com.ricozknow.common.TenantContext;
import com.ricozknow.tenant.Tenant;
import com.ricozknow.tenant.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Spec section 20 "Expiry Scheduler": identifies published article versions
 * whose expires_at has passed and raises a review signal. This does NOT
 * auto-archive content — expiry is an editorial nudge, not an autonomous
 * publishing action (explicitly out of scope per "Won't Have: Autonomous AI
 * publishing" / the spec's broader stance that governance stays human-driven).
 *
 * Runs per-tenant since TenantContext/RLS assume a single resolved tenant
 * per unit of work; iterating tenants here is the batch-job equivalent of
 * the per-request tenant resolution used everywhere else.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExpiryScheduler {

    private final TenantRepository tenantRepository;
    private final ArticleVersionRepository versionRepository;
    private final AuditService auditService;

    @Scheduled(cron = "0 0 3 * * *") // daily at 03:00
    public void flagExpiredContent() {
        List<Tenant> tenants = tenantRepository.findAll();
        for (Tenant tenant : tenants) {
            if (tenant.getStatus() != Tenant.TenantStatus.ACTIVE) {
                continue;
            }
            flagForTenant(tenant.getId());
        }
    }

    @Transactional
    void flagForTenant(java.util.UUID tenantId) {
        TenantContext.set(tenantId);
        try {
            List<ArticleVersion> expired = versionRepository.findByTenantIdAndStatusAndExpiresAtBefore(
                    tenantId, VersionStatus.PUBLISHED, Instant.now());

            for (ArticleVersion version : expired) {
                // "review_due" is picked up by the Phase 5 knowledge-gap/stale-content
                // dashboards; recorded here as an audit event so there's a durable,
                // queryable trail even before that dashboard exists.
                auditService.record("ArticleVersion", version.getId(), "REVIEW_DUE",
                        null, "expired_at=" + version.getExpiresAt());
            }

            if (!expired.isEmpty()) {
                log.info("Flagged {} expired article version(s) for review in tenant {}", expired.size(), tenantId);
            }
        } finally {
            TenantContext.clear();
        }
    }
}
