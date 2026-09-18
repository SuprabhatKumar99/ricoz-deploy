package com.ricozknow.analytics.dto;

public record KnowledgeGapResponse(
        String query,
        int volumeCount,
        int zeroResultCount,
        int unsuccessfulCount,
        int gapScore
) {
}
