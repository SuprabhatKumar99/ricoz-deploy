package com.ricozknow.analytics.dto;

public record DeflectionSummaryResponse(
        int estimatedDeflectionCount,
        int confirmedDeflectionCount,
        int supportContactCount
) {
}
