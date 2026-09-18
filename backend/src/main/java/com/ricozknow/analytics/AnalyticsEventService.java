package com.ricozknow.analytics;

import com.ricozknow.common.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsEventService {

    private final AnalyticsEventRepository repository;

    /**
     * Fire-and-forget style: never let event capture failures break the
     * portal request that triggered them (spec section 23: analytics is a
     * derived, best-effort store — raw events matter, but not at the cost
     * of availability).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AnalyticsEventType type, String anonymousSessionId, UUID userId,
                        UUID articleId, UUID articleVersionId, String metadataJson) {
        try {
            AnalyticsEvent event = new AnalyticsEvent();
            event.setTenantId(TenantContext.get());
            event.setEventType(type);
            event.setAnonymousSessionId(anonymousSessionId);
            event.setUserId(userId);
            event.setArticleId(articleId);
            event.setArticleVersionId(articleVersionId);
            event.setMetadata(metadataJson);
            repository.save(event);
        } catch (Exception ex) {
            log.error("Failed to record analytics event {} for article {}", type, articleId, ex);
        }
    }
}
