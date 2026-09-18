package com.ricozknow.agent;

import com.ricozknow.agent.dto.AgentArticleResponse;
import com.ricozknow.article.Article;
import com.ricozknow.article.ArticleRepository;
import com.ricozknow.article.ArticleStatus;
import com.ricozknow.article.Visibility;
import com.ricozknow.audit.AuditService;
import com.ricozknow.common.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

/**
 * The Agent API is the one place that closes the "authenticated article
 * visibility" gap documented since Phase 3: callers here have real identity
 * (a staff account or an AGENT_VIEWER service account), so both PUBLIC and
 * AUTHENTICATED visibility content is in scope. PRIVATE never is — that tier
 * stays internal-knowledge-studio-only regardless of caller.
 */
@Service
@RequiredArgsConstructor
public class AgentArticleService {

    private static final Set<Visibility> AGENT_VISIBLE = Set.of(Visibility.PUBLIC, Visibility.AUTHENTICATED);

    private final ArticleRepository articleRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public AgentArticleResponse getById(UUID articleId) {
        Article article = articleRepository.findByTenantIdAndId(TenantContext.get(), articleId)
                .filter(a -> a.getStatus() == ArticleStatus.PUBLISHED)
                .filter(a -> AGENT_VISIBLE.contains(a.getVisibility()))
                .orElseThrow(() -> new AgentArticleNotFoundException(articleId));

        auditService.record("Article", articleId, "AGENT_ARTICLE_VIEWED", null, null);
        return AgentArticleResponse.from(article, article.getActiveVersion());
    }
}
