package com.ricozknow.search;

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
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.indices.DeleteIndexRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Exercises spec section 23's disaster-recovery claim directly: "Index can be
 * rebuilt from PostgreSQL." This is Phase 7's "search recovery testing" —
 * simulating total OpenSearch data loss and proving the admin-triggered
 * rebuild restores full search functionality from the system of record.
 */
@SpringBootTest
class SearchIndexRecoveryIT extends OpenSearchIntegrationTestBase {

    @Autowired
    private TenantProvisioningService tenantProvisioningService;
    @Autowired
    private ArticleService articleService;
    @Autowired
    private SearchIndexingService indexingService;
    @Autowired
    private SearchService searchService;
    @Autowired
    private IndexRebuildService indexRebuildService;
    @Autowired
    private OpenSearchClient openSearchClient;

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
    void indexSurvivesTotalOpenSearchDataLossViaRebuildFromPostgres() throws Exception {
        Article article = articleService.createDraft(new ArticleCreateRequest(
                "Disaster recovery drill", "dr-drill-" + UUID.randomUUID(), null,
                parse("{\"blocks\":[{\"type\":\"paragraph\",\"text\":\"content\"}]}")));
        ArticleVersion v1 = articleService.versionHistory(article.getId()).get(0);
        articleService.submitForReview(article.getId(), v1.getId());
        articleService.review(article.getId(), v1.getId(), new ReviewDecisionRequest(true, "ok"));
        articleService.setVisibility(article.getId(), Visibility.PUBLIC);
        article = articleService.get(article.getId());

        indexingService.indexVersion(tenantId, article.getId(), article.getActiveVersion().getId());
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(searchService.search("drill", null, 0, 10, null).results()).isNotEmpty());

        // Simulate total OpenSearch data loss.
        openSearchClient.indices().delete(DeleteIndexRequest.of(b -> b.index(OpenSearchIndexInitializer.INDEX_NAME)));
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            // Search must degrade gracefully (empty results), never throw, while the
            // index is gone — Postgres remains the source of truth throughout.
            assertThat(searchService.search("drill", null, 0, 10, null).results()).isEmpty();
        });

        // The recovery action: rebuild entirely from Postgres.
        int reindexed = indexRebuildService.rebuildForCurrentTenant();
        assertThat(reindexed).isEqualTo(1);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(searchService.search("drill", null, 0, 10, null).results()).isNotEmpty());
    }
}
