package com.ricozknow.article.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;

public record UpdateDraftContentRequest(
        @NotNull JsonNode content,
        String changeSummary
) {
}