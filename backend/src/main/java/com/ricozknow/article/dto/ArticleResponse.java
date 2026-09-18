package com.ricozknow.article.dto;

import com.ricozknow.article.Article;
import com.ricozknow.article.ArticleStatus;
import com.ricozknow.article.Visibility;

import java.util.UUID;

public record ArticleResponse(
        UUID id,
        String slug,
        String title,
        ArticleStatus status,
        Visibility visibility,
        UUID categoryId,
        UUID activeVersionId,
        Integer activeVersionNumber
) {
    public static ArticleResponse from(Article article) {
        var activeVersion = article.getActiveVersion();

        return new ArticleResponse(
                article.getId(),
                article.getSlug(),
                article.getTitle(),
                article.getStatus(),
                article.getVisibility(),
                article.getCategory() != null
                        ? article.getCategory().getId()
                        : null,
                activeVersion != null
                        ? activeVersion.getId()
                        : null,
                activeVersion != null
                        ? activeVersion.getVersionNumber()
                        : null
        );
    }
}
