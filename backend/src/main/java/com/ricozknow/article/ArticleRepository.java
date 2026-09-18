package com.ricozknow.article;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ArticleRepository extends JpaRepository<Article, UUID> {

    @EntityGraph(attributePaths = {"activeVersion"})
    Optional<Article> findByTenantIdAndId(UUID tenantId, UUID id);

    @EntityGraph(attributePaths = {"activeVersion"})
    Optional<Article> findByTenantIdAndSlugAndStatus(UUID tenantId, String slug, ArticleStatus status);
    
    @EntityGraph(attributePaths = {"activeVersion"})
    Page<Article> findByTenantId(UUID tenantId, Pageable pageable);

    @EntityGraph(attributePaths = {"activeVersion"})
    Page<Article> findByTenantIdAndStatus(UUID tenantId, ArticleStatus status, Pageable pageable);
    
    @EntityGraph(attributePaths = {"activeVersion"})
    Page<Article> findByTenantIdAndStatusAndVisibility(UUID tenantId, ArticleStatus status, Visibility visibility, Pageable pageable);
   
    @EntityGraph(attributePaths = {"activeVersion"})
    Page<Article> findByTenantIdAndStatusAndVisibilityAndCategoryId(
            UUID tenantId, ArticleStatus status, Visibility visibility, UUID categoryId, Pageable pageable);
    
    boolean existsByTenantIdAndSlug(UUID tenantId, String slug);
}
