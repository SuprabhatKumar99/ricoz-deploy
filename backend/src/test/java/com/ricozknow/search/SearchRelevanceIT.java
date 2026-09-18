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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
class SearchRelevanceIT extends OpenSearchIntegrationTestBase {

    @Autowired
    private TenantProvisioningService tenantProvisioningService;
    @Autowired
    private ArticleService articleService;
    @Autowired
    private SearchIndexingService indexingService;
    @Autowired
    private SearchService searchService;

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
    void titleMatchOutranksBodyOnlyMatch() {
        // "vpn" only in the body of an otherwise-unrelated article...
        Article bodyOnly = publish("Office wifi guide",
                "{\"blocks\":[{\"type\":\"paragraph\",\"text\":\"Also see the vpn client if working remotely.\"}]}");
        // ...versus an article whose title is literally about VPN setup.
        Article titleMatch = publish("VPN setup guide",
                "{\"blocks\":[{\"type\":\"paragraph\",\"text\":\"Install the client and connect.\"}]}");

        indexingService.indexVersion(tenantId, bodyOnly.getId(), bodyOnly.getActiveVersion().getId());
        indexingService.indexVersion(tenantId, titleMatch.getId(), titleMatch.getActiveVersion().getId());

        // OpenSearch indexing is near-real-time, not immediate; poll briefly rather
        // than sleeping a fixed guess.
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            var result = searchService.search("vpn", null, 0, 10, null);
            assertThat(result.results()).hasSize(2);
            assertThat(result.results().get(0).articleId()).isEqualTo(titleMatch.getId());
        });
    }

    private Article publish(String title, String content) {
        Article article = articleService.createDraft(
                new ArticleCreateRequest(title, "slug-" + UUID.randomUUID(), null, parse(content)));
        ArticleVersion v1 = articleService.versionHistory(article.getId()).get(0);
        articleService.submitForReview(article.getId(), v1.getId());
        articleService.review(article.getId(), v1.getId(), new ReviewDecisionRequest(true, "ok"));
        articleService.setVisibility(article.getId(), Visibility.PUBLIC);
        return articleService.get(article.getId());
    }
}
