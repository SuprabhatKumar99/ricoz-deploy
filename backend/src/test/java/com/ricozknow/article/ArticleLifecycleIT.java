package com.ricozknow.article;

import com.ricozknow.article.dto.ArticleCreateRequest;
import com.ricozknow.article.dto.ReviewDecisionRequest;
import com.ricozknow.article.dto.UpdateDraftContentRequest;
import static com.ricozknow.TestJson.parse;
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

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest
class ArticleLifecycleIT {

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

    private static final String VALID_CONTENT =
            "{\"blocks\":[{\"type\":\"heading\",\"level\":2,\"text\":\"Reset\"},{\"type\":\"paragraph\",\"text\":\"Steps...\"}]}";

    @BeforeEach
    void setUpTenantAndPrincipal() {
        Tenant tenant = tenantProvisioningService.provision("Acme", "acme-" + UUID.randomUUID(), "acme-" + UUID.randomUUID());
        TenantContext.set(tenant.getId());

        AuthenticatedUser editor = new AuthenticatedUser(
                UUID.randomUUID(), tenant.getId(), "editor@acme.test", null, Set.of("EDITOR"), true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(editor, null, editor.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void draftSubmitApprovePublishesAndLocksTheVersion() {
        Article article = articleService.createDraft(
                new ArticleCreateRequest("How to reset password", "reset-password-" + UUID.randomUUID(), null, parse(VALID_CONTENT)));
        assertThat(article.getStatus()).isEqualTo(ArticleStatus.DRAFT);

        ArticleVersion v1 = articleService.versionHistory(article.getId()).get(0);
        articleService.submitForReview(article.getId(), v1.getId());

        articleService.review(article.getId(), v1.getId(), new ReviewDecisionRequest(true, "looks good"));

        Article republished = articleService.get(article.getId());
        assertThat(republished.getStatus()).isEqualTo(ArticleStatus.PUBLISHED);
        assertThat(republished.getActiveVersion().getId()).isEqualTo(v1.getId());

        // Published versions must be immutable.
        assertThatThrownBy(() -> articleService.updateDraftContent(article.getId(), v1.getId(),
                new UpdateDraftContentRequest(parse(VALID_CONTENT), "sneaky edit")))
                .isInstanceOf(InvalidArticleStateException.class);
    }

    @Test
    void rejectedReviewReturnsVersionToDraft() {
        Article article = articleService.createDraft(
                new ArticleCreateRequest("Billing FAQ", "billing-faq-" + UUID.randomUUID(), null, parse(VALID_CONTENT)));
        ArticleVersion v1 = articleService.versionHistory(article.getId()).get(0);

        articleService.submitForReview(article.getId(), v1.getId());
        articleService.review(article.getId(), v1.getId(), new ReviewDecisionRequest(false, "needs more detail"));

        Article reverted = articleService.get(article.getId());
        assertThat(reverted.getStatus()).isEqualTo(ArticleStatus.DRAFT);
        assertThat(articleService.versionHistory(article.getId()).get(0).getStatus()).isEqualTo(VersionStatus.DRAFT);
    }

    @Test
    void invalidContentIsRejectedBeforePersisting() {
        assertThatThrownBy(() -> articleService.createDraft(
                new ArticleCreateRequest("Bad content", "bad-content-" + UUID.randomUUID(), null, parse("{\"blocks\":[{\"type\":\"script\"}]}"))))
                .isInstanceOf(InvalidArticleContentException.class);
    }
}
