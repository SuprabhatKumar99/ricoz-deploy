package com.ricozknow.analytics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * SEARCH_PERFORMED events store a small free-form JSON metadata blob (see
 * SearchService.recordSearchEvent) rather than a dedicated column, since the
 * event schema is shared across all event types. This is the one place that
 * knows how to read it back out.
 */
public record SearchEventMetadata(String query, boolean zeroResult, long resultCount) {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static SearchEventMetadata parse(String json) {
        if (json == null || json.isBlank()) {
            return new SearchEventMetadata(null, false, 0);
        }
        try {
            JsonNode node = MAPPER.readTree(json);
            String query = node.has("query") ? node.get("query").asText() : null;
            boolean zeroResult = node.has("zeroResult") && node.get("zeroResult").asBoolean();
            long resultCount = node.has("resultCount") ? node.get("resultCount").asLong() : 0;
            return new SearchEventMetadata(query, zeroResult, resultCount);
        } catch (Exception ex) {
            return new SearchEventMetadata(null, false, 0);
        }
    }
}
