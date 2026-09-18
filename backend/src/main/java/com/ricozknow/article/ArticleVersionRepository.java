package com.ricozknow.article;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ArticleVersionRepository extends JpaRepository<ArticleVersion, UUID> {
    List<ArticleVersion> findByTenantIdAndArticleIdOrderByVersionNumberDesc(UUID tenantId, UUID articleId);
    Optional<ArticleVersion> findByTenantIdAndId(UUID tenantId, UUID id);
    Optional<ArticleVersion> findTopByTenantIdAndArticleIdOrderByVersionNumberDesc(UUID tenantId, UUID articleId);
    List<ArticleVersion> findByTenantIdAndStatusAndExpiresAtBefore(
            UUID tenantId, VersionStatus status, java.time.Instant cutoff);
    List<ArticleVersion> findByTenantIdAndStatusAndExpiresAtBetween(
            UUID tenantId, VersionStatus status, java.time.Instant from, java.time.Instant to);
}
