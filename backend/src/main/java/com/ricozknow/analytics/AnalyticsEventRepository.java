package com.ricozknow.analytics;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEvent, UUID> {
    List<AnalyticsEvent> findByTenantIdAndEventTypeAndOccurredAtBetween(
            UUID tenantId, AnalyticsEventType eventType, Instant from, Instant to);
}
