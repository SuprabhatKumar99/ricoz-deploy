package com.ricozknow.analytics.dto;

import java.time.Instant;
import java.util.UUID;

public record StaleContentResponse(
        UUID articleId,
        UUID versionId,
        String title,
        Instant expiresAt,
        boolean alreadyExpired
) {
}
