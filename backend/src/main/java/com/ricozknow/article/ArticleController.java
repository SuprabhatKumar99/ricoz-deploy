package com.ricozknow.article;

import com.ricozknow.article.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    @GetMapping
    public Page<ArticleResponse> list(@RequestParam(required = false) ArticleStatus status, Pageable pageable) {
        return articleService.list(status, pageable).map(ArticleResponse::from);
    }

    @GetMapping("/{id}")
    public ArticleResponse get(@PathVariable UUID id) {
        return ArticleResponse.from(articleService.get(id));
    }

    @GetMapping("/{id}/versions")
    public List<ArticleVersionResponse> versions(@PathVariable UUID id) {
        return articleService.versionHistory(id).stream().map(ArticleVersionResponse::from).toList();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<ArticleResponse> create(@Valid @RequestBody ArticleCreateRequest request) {
        return ResponseEntity.ok(ArticleResponse.from(articleService.createDraft(request)));
    }

    @PutMapping("/{articleId}/versions/{versionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ArticleVersionResponse updateDraft(@PathVariable UUID articleId,
                                               @PathVariable UUID versionId,
                                               @Valid @RequestBody UpdateDraftContentRequest request) {
        return ArticleVersionResponse.from(articleService.updateDraftContent(articleId, versionId, request));
    }

    @PostMapping("/{articleId}/versions")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ArticleVersionResponse newDraftVersion(@PathVariable UUID articleId) {
        return ArticleVersionResponse.from(articleService.createNewDraftVersion(articleId));
    }

    @PostMapping("/{articleId}/versions/{versionId}/submit")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ArticleVersionResponse submitForReview(@PathVariable UUID articleId, @PathVariable UUID versionId) {
        return ArticleVersionResponse.from(articleService.submitForReview(articleId, versionId));
    }

    @PostMapping("/{articleId}/versions/{versionId}/review")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEWER')")
    public ArticleVersionResponse review(@PathVariable UUID articleId,
                                          @PathVariable UUID versionId,
                                          @Valid @RequestBody ReviewDecisionRequest request) {
        return ArticleVersionResponse.from(articleService.review(articleId, versionId, request));
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<Void> archive(@PathVariable UUID id) {
        articleService.archive(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/visibility")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<Void> setVisibility(@PathVariable UUID id, @RequestParam Visibility visibility) {
        articleService.setVisibility(id, visibility);
        return ResponseEntity.noContent().build();
    }
}
