package com.ricozknow.e2e;

import com.ricozknow.agent.AgentArticleService;
import com.ricozknow.analytics.AnalyticsAggregationService;
import com.ricozknow.analytics.AnalyticsEventService;
import com.ricozknow.analytics.AnalyticsEventType;
import com.ricozknow.article.*;
import static com.ricozknow.TestJson.parse;
import com.ricozknow.article.dto.ArticleCreateRequest;
import com.ricozknow.article.dto.ReviewDecisionRequest;
import com.ricozknow.common.TenantContext;
import com.ricozknow.portal.PortalArticleService;
import com.ricozknow.search.OpenSearchIntegrationTestBase;
import com.ricozknow.search.SearchService;
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

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Drives the exact flow named in SPEC-001's "End-to-End MVP Acceptance"
 * milestone, top to bottom, against real Postgres + Redis + OpenSearch
 * (via OpenSearchIntegrationTestBase) rather than mocks:
 *
 *   Tenant Created -> User Created -> Article Created -> Article Reviewed ->
 *   Article Published -> OpenSearch Indexed -> Customer Searches ->
 *   Customer Reads -> Events Recorded -> Analytics Updated ->
 *   (no gap, since the search succeeded) -> Agent reads the same article
 *
 * This is the one test in the suite that proves every phase's pieces still
 * fit together as a whole system, not just in isolation — including the real
 * async publish -> Redis -> IndexingWorker -> OpenSearch pipeline from Phase 4,
 * which every other test bypasses by calling SearchIndexingService directly.
 */
@SpringBootTest
class EndToEndAcceptanceIT extends OpenSearchIntegrationTestBase {

    @Autowired
    private TenantProvisioningService tenantProvisioningService;
    @Autowired
    private ArticleService articleService;
    @Autowired
    private SearchService searchService;
    @Autowired
    private PortalArticleService portalArticleService;
    @Autowired
    private AgentArticleService agentArticleService;
    @Autowired
    private AnalyticsEventService analyticsEventService;
    @Autowired
    private AnalyticsAggregationService aggregationService;
    @Autowired
    private com.ricozknow.analytics.AnalyticsDashboardService analyticsDashboardService;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        // 1. Tenant Created
        Tenant tenant = tenantProvisioningService.provision(
                "Acme Corp", "acme-" + UUID.randomUUID(), "acme-" + UUID.randomUUID());
        tenantId = tenant.getId();
        TenantContext.set(tenantId);

        // 2. User Created (the editor who will author the article)
        AuthenticatedUser editor = new AuthenticatedUser(
                UUID.randomUUID(), tenantId, "editor@acme.test", null, Set.of("ADMIN"), true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(editor, null, editor.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void fullKnowledgeLifecycleFromCreationToAnalytics() {
        // 3. Article Created
        Article article = articleService.createDraft(new ArticleCreateRequest(
                "How to reset your password",
                "e2e-reset-password-" + UUID.randomUUID(),
                null,
                parse("{\"blocks\":[{\"type\":\"heading\",\"level\":2,\"text\":\"Reset your password\"},"
                        + "{\"type\":\"paragraph\",\"text\":\"Open account settings and click reset.\"}]}")));
        ArticleVersion v1 = articleService.versionHistory(article.getId()).get(0);

        // 4. Article Reviewed
        articleService.submitForReview(article.getId(), v1.getId());
        articleService.review(article.getId(), v1.getId(), new ReviewDecisionRequest(true, "approved"));

        // 5. Article Published (this also enqueues the real Redis index job)
        article = articleService.get(article.getId());
        assertThat(article.getStatus()).isEqualTo(ArticleStatus.PUBLISHED);
        articleService.setVisibility(article.getId(), Visibility.PUBLIC);
        final UUID articleId = article.getId();

        // 6. OpenSearch Indexed — via the *real* async worker consuming the *real*
        // Redis queue, not a direct synchronous call. This is the whole Phase 4
        // pipeline actually running end to end.
        await().atMost(20, TimeUnit.SECONDS).untilAsserted(() -> {
            var results = searchService.search("reset password", null, 0, 10, "customer-session-1").results();
            assertThat(results).anyMatch(r -> r.articleId().equals(articleId));
        });

        // 7. Customer Searches (recorded as part of the call above) +
        // 8. Customer Reads
        var detail = portalArticleService.getBySlug(article.getSlug(), "customer-session-1");
        assertThat(detail.title()).contains("reset your password".split(" ")[1], "password"); // sanity check on payload

        // 9. Events Recorded — feedback, on top of the search + view events already fired.
        portalArticleService.recordFeedback(articleId, true, "customer-session-1");

        // 10. Analytics Updated
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        aggregationService.aggregateForDate(tenantId, today);

        // 11. Knowledge Gap Identified — inverted here on purpose: this search
        // *succeeded* (real result, real subsequent view), so per spec section 17's
        // formula it must NOT surface as a high-scoring gap the way a genuinely
        // unanswered query would.
        var gaps = analyticsDashboardService.knowledgeGaps(1, 20);
        var thisQueryGap = gaps.stream().filter(g -> g.query().equals("reset password")).findFirst();
        assertThat(thisQueryGap).isPresent();
        assertThat(thisQueryGap.get().zeroResultCount()).isZero();
        assertThat(thisQueryGap.get().unsuccessfulCount()).isZero();

        // Bonus: Agent API reaches the same published knowledge independently.
        var agentView = agentArticleService.getById(articleId);
        assertThat(agentView.title()).isEqualTo(article.getTitle());
    }
}
