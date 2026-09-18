package com.ricozknow.analytics;

import com.ricozknow.analytics.dto.ArticlePerformanceResponse;
import com.ricozknow.analytics.dto.DeflectionSummaryResponse;
import com.ricozknow.analytics.dto.KnowledgeGapResponse;
import com.ricozknow.analytics.dto.StaleContentResponse;
import com.ricozknow.common.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AnalyticsController {

    private static final int DEFAULT_WINDOW_DAYS = 30;
    private static final int DEFAULT_GAP_LIMIT = 20;

    private final AnalyticsDashboardService dashboardService;
    private final AnalyticsAggregationService aggregationService;

    @GetMapping("/articles/performance")
    public List<ArticlePerformanceResponse> articlePerformance(
            @RequestParam(defaultValue = "" + DEFAULT_WINDOW_DAYS) int days) {
        return dashboardService.articlePerformance(days);
    }

    @GetMapping("/knowledge-gaps")
    public List<KnowledgeGapResponse> knowledgeGaps(
            @RequestParam(defaultValue = "" + DEFAULT_WINDOW_DAYS) int days,
            @RequestParam(defaultValue = "" + DEFAULT_GAP_LIMIT) int limit) {
        return dashboardService.knowledgeGaps(days, limit);
    }

    @GetMapping("/deflection")
    public DeflectionSummaryResponse deflection(@RequestParam(defaultValue = "" + DEFAULT_WINDOW_DAYS) int days) {
        return dashboardService.deflectionSummary(days);
    }

    @GetMapping("/stale-content")
    public List<StaleContentResponse> staleContent(@RequestParam(defaultValue = "30") int horizonDays) {
        return dashboardService.staleContent(horizonDays);
    }

    /**
     * Manual trigger for the nightly aggregation job — useful for ops
     * (backfilling a missed night) and for testing without waiting for 03:30.
     * Idempotent: safe to call repeatedly for the same date.
     */
    @PostMapping("/aggregate")
    public Map<String, String> aggregateNow(@RequestParam(required = false) LocalDate date) {
        LocalDate target = date != null ? date : LocalDate.now().minusDays(1);
        aggregationService.aggregateForDate(TenantContext.get(), target);
        return Map.of("status", "aggregated", "date", target.toString());
    }
}
