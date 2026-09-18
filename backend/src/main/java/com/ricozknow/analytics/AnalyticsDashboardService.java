package com.ricozknow.analytics;

import com.ricozknow.analytics.dto.ArticlePerformanceResponse;
import com.ricozknow.analytics.dto.DeflectionSummaryResponse;
import com.ricozknow.analytics.dto.KnowledgeGapResponse;
import com.ricozknow.analytics.dto.StaleContentResponse;
import com.ricozknow.article.Article;
import com.ricozknow.article.ArticleRepository;
import com.ricozknow.article.ArticleVersion;
import com.ricozknow.article.ArticleVersionRepository;
import com.ricozknow.article.VersionStatus;
import com.ricozknow.common.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Sums the daily rollups produced by AnalyticsAggregationService over a
 * requested window and joins back to human-readable article titles. Kept
 * separate from the aggregation service so "how the numbers are computed"
 * and "how they're presented" can change independently.
 */
@Service
@RequiredArgsConstructor
public class AnalyticsDashboardService {

    private static final int DEFAULT_STALE_HORIZON_DAYS = 30;

    private final DailyArticleStatRepository articleStatRepository;
    private final DailySearchStatRepository searchStatRepository;
    private final DailyDeflectionStatRepository deflectionStatRepository;
    private final ArticleRepository articleRepository;
    private final ArticleVersionRepository versionRepository;

    @Transactional(readOnly = true)
    public List<ArticlePerformanceResponse> articlePerformance(int days) {
        UUID tenantId = TenantContext.get();
        var stats = articleStatRepository.findByTenantIdAndStatDateBetween(
                tenantId, LocalDate.now(ZoneOffset.UTC).minusDays(days), LocalDate.now(ZoneOffset.UTC));

        Map<UUID, int[]> totals = new HashMap<>(); // [views, helpful, unhelpful]
        for (DailyArticleStat stat : stats) {
            int[] counts = totals.computeIfAbsent(stat.getArticleId(), k -> new int[3]);
            counts[0] += stat.getViews();
            counts[1] += stat.getHelpfulCount();
            counts[2] += stat.getUnhelpfulCount();
        }

        return totals.entrySet().stream()
                .map(e -> {
                    Article article = articleRepository.findByTenantIdAndId(tenantId, e.getKey()).orElse(null);
                    String title = article != null ? article.getTitle() : "(deleted article)";
                    return new ArticlePerformanceResponse(e.getKey(), title, e.getValue()[0], e.getValue()[1], e.getValue()[2]);
                })
                .sorted(Comparator.comparingInt(ArticlePerformanceResponse::views).reversed())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<KnowledgeGapResponse> knowledgeGaps(int days, int limit) {
        UUID tenantId = TenantContext.get();
        var stats = searchStatRepository.findByTenantIdAndStatDateBetween(
                tenantId, LocalDate.now(ZoneOffset.UTC).minusDays(days), LocalDate.now(ZoneOffset.UTC));

        Map<String, int[]> totals = new HashMap<>(); // [volume, zeroResult, unsuccessful]
        for (DailySearchStat stat : stats) {
            int[] counts = totals.computeIfAbsent(stat.getQueryText(), k -> new int[3]);
            counts[0] += stat.getVolumeCount();
            counts[1] += stat.getZeroResultCount();
            counts[2] += stat.getUnsuccessfulCount();
        }

        // Spec section 17's heuristic, applied verbatim:
        // Gap Score = zero-result x 3 + unsuccessful x 2 + volume x 1
        return totals.entrySet().stream()
                .map(e -> {
                    int volume = e.getValue()[0], zeroResult = e.getValue()[1], unsuccessful = e.getValue()[2];
                    int gapScore = zeroResult * 3 + unsuccessful * 2 + volume;
                    return new KnowledgeGapResponse(e.getKey(), volume, zeroResult, unsuccessful, gapScore);
                })
                .sorted(Comparator.comparingInt(KnowledgeGapResponse::gapScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DeflectionSummaryResponse deflectionSummary(int days) {
        UUID tenantId = TenantContext.get();
        var stats = deflectionStatRepository.findByTenantIdAndStatDateBetween(
                tenantId, LocalDate.now(ZoneOffset.UTC).minusDays(days), LocalDate.now(ZoneOffset.UTC));

        int estimated = 0, confirmed = 0, contacts = 0;
        for (DailyDeflectionStat stat : stats) {
            estimated += stat.getEstimatedDeflectionCount();
            confirmed += stat.getConfirmedDeflectionCount();
            contacts += stat.getSupportContactCount();
        }
        return new DeflectionSummaryResponse(estimated, confirmed, contacts);
    }

    /** Spec section 9 rule 9 / section 20: published content nearing or past its review date. */
    @Transactional(readOnly = true)
    public List<StaleContentResponse> staleContent(int horizonDays) {
        UUID tenantId = TenantContext.get();
        int horizon = horizonDays > 0 ? horizonDays : DEFAULT_STALE_HORIZON_DAYS;
        Instant now = Instant.now();
        Instant horizonEnd = now.plusSeconds(horizon * 86400L);

        List<ArticleVersion> expiring = versionRepository.findByTenantIdAndStatusAndExpiresAtBetween(
                tenantId, VersionStatus.PUBLISHED, Instant.EPOCH, horizonEnd);

        return expiring.stream()
                .map(v -> new StaleContentResponse(
                        v.getArticle().getId(), v.getId(), v.getArticle().getTitle(),
                        v.getExpiresAt(), v.getExpiresAt().isBefore(now)))
                .sorted(Comparator.comparing(StaleContentResponse::expiresAt))
                .collect(Collectors.toList());
    }
}
