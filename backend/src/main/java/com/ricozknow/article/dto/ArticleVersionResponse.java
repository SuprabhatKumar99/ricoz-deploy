package com.ricozknow.article.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.ricozknow.article.ArticleVersion;
import com.ricozknow.article.VersionStatus;

import java.time.Instant;
import java.util.UUID;

public record ArticleVersionResponse(
        UUID id,
        int versionNumber,
        JsonNode content,
        String changeSummary,
        VersionStatus status,
        UUID createdBy,
        UUID reviewedBy,
        Instant reviewedAt,
        Instant publishedAt,
        Instant expiresAt
) {

    public static ArticleVersionResponse from(ArticleVersion v) {
        return new ArticleVersionResponse(
                v.getId(),
                v.getVersionNumber(),
                v.getContent(),
                v.getChangeSummary(),
                v.getStatus(),
                v.getCreatedBy(),
                v.getReviewedBy(),
                v.getReviewedAt(),
                v.getPublishedAt(),
                v.getExpiresAt()
        );
    }
}