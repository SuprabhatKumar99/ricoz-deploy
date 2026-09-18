package com.ricozknow.portal.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.ricozknow.article.Article;
import com.ricozknow.article.ArticleVersion;

import java.util.UUID;

public record PortalArticleDetail(
        UUID id,
        String slug,
        String title,
        UUID categoryId,
        JsonNode content,
        int versionNumber
) {

    public static PortalArticleDetail from(
            Article article,
            ArticleVersion version) {

        return new PortalArticleDetail(
                article.getId(),
                article.getSlug(),
                article.getTitle(),
                article.getCategory() != null
                        ? article.getCategory().getId()
                        : null,
                version.getContent(),
                version.getVersionNumber()
        );
    }
}