package com.ricozknow.search.dto;

import java.util.List;
import java.util.UUID;

public record SearchResultResponse(
        String query,
        List<SearchHit> results,
        long total
) {
    public record SearchHit(
            UUID articleId,
            String slug,
            String title,
            String excerpt,
            UUID categoryId,
            double score
    ) {
    }
}
