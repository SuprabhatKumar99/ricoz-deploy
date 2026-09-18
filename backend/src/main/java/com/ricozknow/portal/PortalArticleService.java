package com.ricozknow.portal;

import com.ricozknow.analytics.AnalyticsEventService;
import com.ricozknow.analytics.AnalyticsEventType;
import com.ricozknow.article.Article;
import com.ricozknow.article.ArticleRepository;
import com.ricozknow.article.ArticleStatus;
import com.ricozknow.article.Visibility;
import com.ricozknow.common.TenantContext;
import com.ricozknow.portal.dto.PortalArticleDetail;
import com.ricozknow.portal.dto.PortalArticleSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * MVP scope note: the spec calls for "Public and authenticated article
 * visibility," but the MVP has no customer identity system — only tenant
 * staff (ADMIN/EDITOR/REVIEWER/AGENT_VIEWER) accounts exist. Until customer
 * accounts/SSO land, this service only ever serves PUBLIC visibility content
 * to the anonymous portal; AUTHENTICATED-visibility articles are reachable
 * through the Agent API (which does have real identity) but not yet through
 * the customer portal itself. Revisit when customer auth is scoped.
 *
 * Search itself now lives entirely in SearchService (Phase 4) — the naive
 * DB-substring search this class used in Phase 3 has been removed, not kept
 * as a fallback, per that code's own comment that it should be replaced.
 */
@Service
@RequiredArgsConstructor
public class PortalArticleService {

    private final ArticleRepository articleRepository;
    private final AnalyticsEventService analyticsEventService;

    @Transactional(readOnly = true)
    public Page<PortalArticleSummary> browse(UUID categoryId, Pageable pageable) {
        UUID tenantId = TenantContext.get();
        Page<Article> page = categoryId != null
                ? articleRepository.findByTenantIdAndStatusAndVisibilityAndCategoryId(
                        tenantId, ArticleStatus.PUBLISHED, Visibility.PUBLIC, categoryId, pageable)
                : articleRepository.findByTenantIdAndStatusAndVisibility(
                        tenantId, ArticleStatus.PUBLISHED, Visibility.PUBLIC, pageable);
        return page.map(PortalArticleSummary::from);
    }

    @Transactional
    public PortalArticleDetail getBySlug(String slug, String anonymousSessionId) {
        Article article = articleRepository.findByTenantIdAndSlugAndStatus(
                        TenantContext.get(), slug, ArticleStatus.PUBLISHED)
                .filter(a -> a.getVisibility() == Visibility.PUBLIC)
                .orElseThrow(() -> new IllegalStateException("Article not found or not publicly visible: " + slug));

        analyticsEventService.record(AnalyticsEventType.ARTICLE_VIEWED, anonymousSessionId, null,
                article.getId(), article.getActiveVersion().getId(), null);

        return PortalArticleDetail.from(article, article.getActiveVersion());
    }

    @Transactional
    public void recordFeedback(UUID articleId, boolean helpful, String anonymousSessionId) {
        Article article = articleRepository.findByTenantIdAndId(TenantContext.get(), articleId)
                .filter(a -> a.getStatus() == ArticleStatus.PUBLISHED && a.getVisibility() == Visibility.PUBLIC)
                .orElseThrow(() -> new IllegalStateException("Article not found or not publicly visible: " + articleId));

        analyticsEventService.record(
                helpful ? AnalyticsEventType.ARTICLE_HELPFUL : AnalyticsEventType.ARTICLE_UNHELPFUL,
                anonymousSessionId, null, article.getId(),
                article.getActiveVersion() != null ? article.getActiveVersion().getId() : null, null);
    }
}

