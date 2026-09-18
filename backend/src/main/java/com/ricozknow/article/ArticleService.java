package com.ricozknow.article;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.ricozknow.article.dto.ArticleCreateRequest;
import com.ricozknow.article.dto.ReviewDecisionRequest;
import com.ricozknow.article.dto.UpdateDraftContentRequest;
import com.ricozknow.audit.AuditService;
import com.ricozknow.category.CategoryRepository;
import com.ricozknow.common.TenantContext;
import com.ricozknow.publishing.IndexJobPublisher;
import com.ricozknow.user.AuthenticatedUser;

import lombok.RequiredArgsConstructor;

/**
 * Owns the article lifecycle end to end (spec section 9):
 *
 *   DRAFT --submit--> IN_REVIEW --approve--> PUBLISHED --expire/archive--> ARCHIVED
 *                         |
 *                       reject
 *                         v
 *                       DRAFT
 *
 * Published versions are immutable: editing a published article always creates
 * a new DRAFT version rather than mutating the published row.
 */
@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final ArticleVersionRepository versionRepository;
    private final CategoryRepository categoryRepository;
    private final ContentValidator contentValidator;
    private final AuditService auditService;
    private final IndexJobPublisher indexJobPublisher;

    @Transactional(readOnly = true)
    public Page<Article> list(ArticleStatus status, Pageable pageable) {
        UUID tenantId = TenantContext.get();
        return status != null
                ? articleRepository.findByTenantIdAndStatus(tenantId, status, pageable)
                : articleRepository.findByTenantId(tenantId, pageable);
    }

    @Transactional(readOnly = true)
    public Article get(UUID id) {
        return articleRepository.findByTenantIdAndId(TenantContext.get(), id)
                .orElseThrow(() -> new ArticleNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<ArticleVersion> versionHistory(UUID articleId) {
        return versionRepository.findByTenantIdAndArticleIdOrderByVersionNumberDesc(TenantContext.get(), articleId);
    }

    @Transactional
    public Article createDraft(ArticleCreateRequest request) {
        UUID tenantId = TenantContext.get();
        contentValidator.validate(request.content());

        if (articleRepository.existsByTenantIdAndSlug(tenantId, request.slug())) {
            throw new InvalidArticleStateException("An article with slug '" + request.slug() + "' already exists");
        }

        Article article = new Article();
        article.setTitle(request.title());
        article.setSlug(request.slug());
        article.setStatus(ArticleStatus.DRAFT);
        article.setCreatedBy(currentUserId());
        if (request.categoryId() != null) {
            article.setCategory(categoryRepository.findByTenantIdAndId(tenantId, request.categoryId())
                    .orElseThrow(() -> new InvalidArticleStateException("Unknown category: " + request.categoryId())));
        }
        article = articleRepository.save(article);

        ArticleVersion version = new ArticleVersion();
        version.setArticle(article);
        version.setVersionNumber(1);
        version.setContent(request.content());
        version.setStatus(VersionStatus.DRAFT);
        version.setCreatedBy(currentUserId());
        versionRepository.save(version);

        auditService.record("Article", article.getId(), "ARTICLE_CREATED", null, request.title());
        return article;
    }

    /** Editing a DRAFT version in place. Never allowed once that version is PUBLISHED. */
    // @Transactional
    // public ArticleVersion updateDraftContent(UUID articleId, UUID versionId, UpdateDraftContentRequest request) {
    //     contentValidator.validate(request.content());
    //     ArticleVersion version = getVersion(articleId, versionId);

    //     if (version.isImmutable()) {
    //         throw new InvalidArticleStateException("Cannot edit a published version; create a new draft version instead");
    //     }

    //     String oldContent = version.getContent();
    //     version.setContent(request.content());
    //     version.setChangeSummary(request.changeSummary());
    //     // Editing a rejected/in-review version resets it to draft for further work.
    //     version.setStatus(VersionStatus.DRAFT);

    //     auditService.record("ArticleVersion", version.getId(), "VERSION_CONTENT_UPDATED", oldContent, request.content());
    //     return version;
    // }

    @Transactional
    public ArticleVersion updateDraftContent(
            UUID articleId,
            UUID versionId,
            UpdateDraftContentRequest request) {

        contentValidator.validate(request.content());

        ArticleVersion version =
                getVersion(articleId, versionId);

        if (version.isImmutable()) {
            throw new InvalidArticleStateException(
                    "Cannot edit a published version; create a new draft version instead"
            );
        }

        JsonNode oldContent = version.getContent();

        version.setContent(
                request.content()
        );

        version.setChangeSummary(
                request.changeSummary()
        );

        version.setStatus(
                VersionStatus.DRAFT
        );

        auditService.record(
                "ArticleVersion",
                version.getId(),
                "VERSION_CONTENT_UPDATED",
                oldContent,
                request.content()
        );

        return version;
    }

    /** Starts a new editable revision on top of the currently published version. */
    @Transactional
    public ArticleVersion createNewDraftVersion(UUID articleId) {
        Article article = get(articleId);
        ArticleVersion latest = versionRepository
                .findTopByTenantIdAndArticleIdOrderByVersionNumberDesc(TenantContext.get(), articleId)
                .orElseThrow(() -> new InvalidArticleStateException("Article has no versions"));

        ArticleVersion draft = new ArticleVersion();
        draft.setArticle(article);
        draft.setVersionNumber(latest.getVersionNumber() + 1);
        draft.setContent(latest.getContent()); // seed from the last version; caller edits from here
        draft.setStatus(VersionStatus.DRAFT);
        draft.setCreatedBy(currentUserId());
        versionRepository.save(draft);

        auditService.record("ArticleVersion", draft.getId(), "DRAFT_VERSION_CREATED", null, draft.getVersionNumber());
        return draft;
    }

    @Transactional
    public ArticleVersion submitForReview(UUID articleId, UUID versionId) {
        Article article = get(articleId);
        ArticleVersion version = getVersion(articleId, versionId);

        if (version.getStatus() != VersionStatus.DRAFT) {
            throw new InvalidArticleStateException("Only draft versions can be submitted for review");
        }

        version.setStatus(VersionStatus.IN_REVIEW);
        article.setStatus(ArticleStatus.IN_REVIEW);

        auditService.record("ArticleVersion", version.getId(), "SUBMITTED_FOR_REVIEW", null, null);
        return version;
    }

    /** Reviewer decision. Approve publishes the version; reject sends it back to draft. */
    @Transactional
    public ArticleVersion review(UUID articleId, UUID versionId, ReviewDecisionRequest request) {
        Article article = get(articleId);
        ArticleVersion version = getVersion(articleId, versionId);

        if (version.getStatus() != VersionStatus.IN_REVIEW) {
            throw new InvalidArticleStateException("Only versions in review can be approved or rejected");
        }

        version.setReviewedBy(currentUserId());
        version.setReviewedAt(Instant.now());

        if (Boolean.TRUE.equals(request.approve())) {
            publishVersion(article, version);
            auditService.record("ArticleVersion", version.getId(), "REVIEW_APPROVED", null, request.comment());
        } else {
            version.setStatus(VersionStatus.DRAFT);
            article.setStatus(ArticleStatus.DRAFT);
            auditService.record("ArticleVersion", version.getId(), "REVIEW_REJECTED", null, request.comment());
        }

        return version;
    }

    /** Publishes a version: marks it immutable, makes it the article's active version, enqueues indexing. */
    private void publishVersion(Article article, ArticleVersion version) {
        version.setStatus(VersionStatus.PUBLISHED);
        version.setPublishedAt(Instant.now());

        article.setStatus(ArticleStatus.PUBLISHED);
        article.setActiveVersion(version);

        auditService.record("Article", article.getId(), "ARTICLE_PUBLISHED", null, version.getVersionNumber());
        indexJobPublisher.enqueueIndexJob(TenantContext.get(), article.getId(), version.getId());
    }

    @Transactional
    public void archive(UUID articleId) {
        Article article = get(articleId);
        article.setStatus(ArticleStatus.ARCHIVED);
        auditService.record("Article", articleId, "ARTICLE_ARCHIVED", null, null);
        // Archived articles must disappear from customer search (spec section 9 rule 8).
        indexJobPublisher.enqueueRemovalJob(TenantContext.get(), articleId);
    }

    // @Transactional
    // public void setVisibility(UUID articleId, com.ricozknow.article.Visibility visibility) {
    //     Article article = get(articleId);
    //     var old = article.getVisibility();
    //     article.setVisibility(visibility);
    //     auditService.record("Article", articleId, "VISIBILITY_CHANGED", old, visibility);
    // }

    @Transactional
    public void setVisibility(
            UUID articleId,
            Visibility visibility
    ) {
        Article article = get(articleId);

        Visibility old = article.getVisibility();

        if (old == visibility) {
            return;
        }

        article.setVisibility(visibility);

        auditService.record(
                "Article",
                articleId,
                "VISIBILITY_CHANGED",
                old,
                visibility
        );

        if (article.getStatus() == ArticleStatus.PUBLISHED
                && article.getActiveVersion() != null) {

            indexJobPublisher.enqueueIndexJob(
                    TenantContext.get(),
                    article.getId(),
                    article.getActiveVersion().getId()
            );
        }
    }

    private ArticleVersion getVersion(UUID articleId, UUID versionId) {
        ArticleVersion version = versionRepository.findByTenantIdAndId(TenantContext.get(), versionId)
                .orElseThrow(() -> new InvalidArticleStateException("Version not found: " + versionId));
        if (!version.getArticle().getId().equals(articleId)) {
            throw new InvalidArticleStateException("Version does not belong to article " + articleId);
        }
        return version;
    }

    private UUID currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser principal) {
            return principal.userId();
        }
        throw new IllegalStateException("No authenticated user in context");
    }
}
