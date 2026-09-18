package com.ricozknow.analytics;

import com.ricozknow.article.*;
import static com.ricozknow.TestJson.parse;
import com.ricozknow.article.dto.ArticleCreateRequest;
import com.ricozknow.article.dto.ReviewDecisionRequest;
import com.ricozknow.common.TenantContext;
import com.ricozknow.tenant.Tenant;
import com.ricozknow.tenant.TenantProvisioningService;
import com.ricozknow.user.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class AnalyticsAggregationServiceIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("ricozknow_test")
            .withUsername("ricozknow")
            .withPassword("ricozknow");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TenantProvisioningService tenantProvisioningService;
    @Autowired
    private ArticleService articleService;
    @Autowired
    private AnalyticsEventService analyticsEventService;
    @Autowired
    private AnalyticsAggregationService aggregationService;
    @Autowired
    private AnalyticsDashboardService dashboardService;

    private static final String CONTENT = "{\"blocks\":[{\"type\":\"paragraph\",\"text\":\"hi\"}]}";

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        Tenant tenant = tenantProvisioningService.provision("Acme", "acme-" + UUID.randomUUID(), "acme-" + UUID.randomUUID());
        tenantId = tenant.getId();
        TenantContext.set(tenantId);
        AuthenticatedUser admin = new AuthenticatedUser(
                UUID.randomUUID(), tenantId, "admin@acme.test", null, Set.of("ADMIN"), true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin, null, admin.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void articleViewsAndFeedbackAreAggregatedPerDay() {
        Article article = publishArticle();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        analyticsEventService.record(AnalyticsEventType.ARTICLE_VIEWED, "s1", null, article.getId(), null, null);
        analyticsEventService.record(AnalyticsEventType.ARTICLE_VIEWED, "s2", null, article.getId(), null, null);
        analyticsEventService.record(AnalyticsEventType.ARTICLE_HELPFUL, "s1", null, article.getId(), null, null);

        aggregationService.aggregateForDate(tenantId, today);

        List<com.ricozknow.analytics.dto.ArticlePerformanceResponse> performance = dashboardService.articlePerformance(1);
        assertThat(performance).hasSize(1);
        assertThat(performance.get(0).views()).isEqualTo(2);
        assertThat(performance.get(0).helpfulCount()).isEqualTo(1);

        // Idempotency: re-running the same date must not double-count.
        aggregationService.aggregateForDate(tenantId, today);
        List<com.ricozknow.analytics.dto.ArticlePerformanceResponse> rerun = dashboardService.articlePerformance(1);
        assertThat(rerun.get(0).views()).isEqualTo(2);
    }

    @Test
    void zeroResultSearchesDominateTheGapScore() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        analyticsEventService.record(AnalyticsEventType.SEARCH_PERFORMED, "s1", null, null, null,
                "{\"query\":\"vpn setup\",\"zeroResult\":true,\"resultCount\":0}");
        analyticsEventService.record(AnalyticsEventType.SEARCH_PERFORMED, "s2", null, null, null,
                "{\"query\":\"vpn setup\",\"zeroResult\":true,\"resultCount\":0}");

        aggregationService.aggregateForDate(tenantId, today);

        var gaps = dashboardService.knowledgeGaps(1, 10);
        assertThat(gaps).hasSize(1);
        // volume=2, zeroResult=2, unsuccessful=2 -> 2*3 + 2*2 + 2 = 12
        assertThat(gaps.get(0).gapScore()).isEqualTo(12);
    }

    private Article publishArticle() {
        Article article = articleService.createDraft(
                new ArticleCreateRequest("Test Article", "test-article-" + UUID.randomUUID(), null, parse(CONTENT)));
        ArticleVersion v1 = articleService.versionHistory(article.getId()).get(0);
        articleService.submitForReview(article.getId(), v1.getId());
        articleService.review(article.getId(), v1.getId(), new ReviewDecisionRequest(true, "ok"));
        return articleService.get(article.getId());
    }
}
