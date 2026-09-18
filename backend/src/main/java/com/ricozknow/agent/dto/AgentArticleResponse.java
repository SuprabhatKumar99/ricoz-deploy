package com.ricozknow.agent.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.ricozknow.article.Article;
import com.ricozknow.article.ArticleVersion;

import java.time.Instant;
import java.util.UUID;

public record AgentArticleResponse(
        UUID id,
        String slug,
        String title,
        UUID categoryId,
        String visibility,
        JsonNode content,
        int versionNumber,
        Instant publishedAt
) {

    public static AgentArticleResponse from(
            Article article,
            ArticleVersion version) {

        return new AgentArticleResponse(
                article.getId(),
                article.getSlug(),
                article.getTitle(),
                article.getCategory() != null
                        ? article.getCategory().getId()
                        : null,
                article.getVisibility().name(),
                version.getContent(),
                version.getVersionNumber(),
                version.getPublishedAt()
        );
    }
}