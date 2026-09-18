package com.ricozknow.article.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ArticleCreateRequest(
        @NotBlank String title,
        @NotBlank String slug,
        UUID categoryId,
        @NotNull JsonNode content
) {
}