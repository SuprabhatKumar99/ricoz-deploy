package com.ricozknow.agent;

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

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest
class AgentArticleServiceIT {

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
    private AgentArticleService agentArticleService;

    private static final String CONTENT = "{\"blocks\":[{\"type\":\"paragraph\",\"text\":\"hi\"}]}";

    @BeforeEach
    void setUp() {
        Tenant tenant = tenantProvisioningService.provision("Acme", "acme-" + UUID.randomUUID(), "acme-" + UUID.randomUUID());
        TenantContext.set(tenant.getId());
        AuthenticatedUser admin = new AuthenticatedUser(
                UUID.randomUUID(), tenant.getId(), "admin@acme.test", null, Set.of("ADMIN"), true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin, null, admin.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    private Article publishWithVisibility(Visibility visibility) {
        Article article = articleService.createDraft(
                new ArticleCreateRequest("Agent test " + visibility, "agent-" + visibility + "-" + UUID.randomUUID(), null, parse(CONTENT)));
        ArticleVersion v1 = articleService.versionHistory(article.getId()).get(0);
        articleService.submitForReview(article.getId(), v1.getId());
        articleService.review(article.getId(), v1.getId(), new ReviewDecisionRequest(true, "ok"));
        articleService.setVisibility(article.getId(), visibility);
        return articleService.get(article.getId());
    }

    @Test
    void agentCanReadPublicAndAuthenticatedArticles() {
        Article pub = publishWithVisibility(Visibility.PUBLIC);
        Article auth = publishWithVisibility(Visibility.AUTHENTICATED);

        assertThat(agentArticleService.getById(pub.getId()).title()).contains("PUBLIC");
        assertThat(agentArticleService.getById(auth.getId()).title()).contains("AUTHENTICATED");
    }

    @Test
    void agentCanNeverReadPrivateArticles() {
        Article priv = publishWithVisibility(Visibility.PRIVATE);
        assertThatThrownBy(() -> agentArticleService.getById(priv.getId()))
                .isInstanceOf(AgentArticleNotFoundException.class);
    }
}
