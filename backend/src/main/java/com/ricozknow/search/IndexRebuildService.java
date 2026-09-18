package com.ricozknow.search;

import com.ricozknow.article.Article;
import com.ricozknow.article.ArticleRepository;
import com.ricozknow.article.ArticleStatus;
import com.ricozknow.audit.AuditService;
import com.ricozknow.common.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spec section 23: "Index can be rebuilt from PostgreSQL." Walks every
 * PUBLISHED article for the current tenant and re-indexes its active version
 * directly (synchronously, not via the Redis queue) — this is an explicit
 * operator action, not something that should silently queue behind other
 * publish traffic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IndexRebuildService {

    private static final int PAGE_SIZE = 100;

    private final ArticleRepository articleRepository;
    private final SearchIndexingService indexingService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public int rebuildForCurrentTenant() {
        var tenantId = TenantContext.get();
        int indexed = 0;
        int pageNumber = 0;
        Pageable pageable = PageRequest.of(pageNumber, PAGE_SIZE);

        var page = articleRepository.findByTenantIdAndStatus(tenantId, ArticleStatus.PUBLISHED, pageable);
        while (!page.isEmpty()) {
            for (Article article : page.getContent()) {
                if (article.getActiveVersion() != null) {
                    indexingService.indexVersion(tenantId, article.getId(), article.getActiveVersion().getId());
                    indexed++;
                }
            }
            if (!page.hasNext()) {
                break;
            }
            pageable = pageable.next();
            page = articleRepository.findByTenantIdAndStatus(tenantId, ArticleStatus.PUBLISHED, pageable);
        }

        log.info("Rebuilt search index for tenant {}: {} article(s) indexed", tenantId, indexed);
        auditService.record("SearchIndex", null, "INDEX_REBUILT", null, indexed);
        return indexed;
    }
}
