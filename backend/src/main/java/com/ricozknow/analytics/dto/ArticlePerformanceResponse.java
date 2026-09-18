package com.ricozknow.analytics.dto;

import java.util.UUID;

public record ArticlePerformanceResponse(
        UUID articleId,
        String title,
        int views,
        int helpfulCount,
        int unhelpfulCount
) {
}
