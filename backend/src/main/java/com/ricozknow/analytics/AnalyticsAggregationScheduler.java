package com.ricozknow.analytics;

import com.ricozknow.tenant.Tenant;
import com.ricozknow.tenant.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;

@Component
@RequiredArgsConstructor
@Slf4j
public class AnalyticsAggregationScheduler {

    private final TenantRepository tenantRepository;
    private final AnalyticsAggregationService aggregationService;

    /** Runs after the expiry scheduler (spec section 20's "Aggregate Analytics Job"). */
    @Scheduled(cron = "0 30 3 * * *") // daily at 03:30
    public void aggregateYesterdayForAllTenants() {
        LocalDate yesterday = LocalDate.now(ZoneOffset.UTC).minusDays(1);
        for (Tenant tenant : tenantRepository.findAll()) {
            if (tenant.getStatus() != Tenant.TenantStatus.ACTIVE) {
                continue;
            }
            try {
                aggregationService.aggregateForDate(tenant.getId(), yesterday);
            } catch (Exception ex) {
                // One tenant's aggregation failure must never block the others';
                // the job is safely re-runnable for that tenant/date on the next pass.
                log.error("Analytics aggregation failed for tenant {} date {}", tenant.getId(), yesterday, ex);
            }
        }
    }
}
