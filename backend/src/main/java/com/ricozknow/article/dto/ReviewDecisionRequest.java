package com.ricozknow.article.dto;

import jakarta.validation.constraints.NotNull;

public record ReviewDecisionRequest(
        @NotNull Boolean approve,
        String comment
) {
}
