package com.ricozknow.search;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ricozknow.analytics.AnalyticsEventService;
import com.ricozknow.analytics.AnalyticsEventType;
import com.ricozknow.article.Visibility;
import com.ricozknow.common.TenantContext;
import com.ricozknow.search.dto.SearchResultResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.opensearch.client.opensearch._types.FieldValue;

/**
 * Implements spec section 12's pipeline: normalization -> synonym expansion ->
 * OpenSearch (weighted by title > headings > keyword > body > category) ->
 * ranked results -> analytics event, including zero-result detection
 * (section 17 builds on the events recorded here in Phase 5).
 *
 * The request body is built as a JSON tree and sent via OpenSearch's raw-JSON
 * request support rather than the fully typed query DSL builders — for a
 * query this shape-dependent, an explicit, readable JSON body is easier to
 * reason about (and keep correct) than deeply nested typed builder lambdas.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final OpenSearchClient client;
    private final SynonymService synonymService;
    private final AnalyticsEventService analyticsEventService;
    private final ObjectMapper objectMapper;

    public SearchResultResponse search(String rawQuery, UUID categoryId, int page, int size,
                                        String anonymousSessionId) {
        return search(rawQuery, categoryId, page, size, anonymousSessionId, Set.of(Visibility.PUBLIC));
    }

    /**
     * @param allowedVisibilities which visibility tiers this caller may see.
     *                            The anonymous portal only ever passes {PUBLIC}.
     *                            The Agent API (real staff/service-account identity)
     *                            passes {PUBLIC, AUTHENTICATED} — see AgentSearchService.
     */
   

    public SearchResultResponse search(String rawQuery, 
                                        UUID categoryId, 
                                        int page, int size,
                                        String anonymousSessionId, 
                                        Set<Visibility> allowedVisibilities
                                    ) {

        String normalizedQuery = rawQuery == null ? "" : rawQuery.trim();

        if (normalizedQuery.isEmpty()) {
            return new SearchResultResponse(rawQuery, List.of(), 0);
        }

        Set<String> expandedTerms = synonymService.expand(normalizedQuery);

        SearchResultResponse result;

        try {
            SearchRequest.Builder request = new SearchRequest.Builder()
                    .index(OpenSearchIndexInitializer.INDEX_NAME)
                    .from(page * size)
                    .size(size)
                    .trackTotalHits(t -> t.enabled(true));

            request.query(q -> q.bool(bool -> {

                for (String term : expandedTerms) {
                    bool.should(s -> s.multiMatch(mm -> mm
                            .query(term)
                            .fields(
                                    "title^4",
                                    "headings^3",
                                    "keywords^2",
                                    "categoryName^1.5",
                                    "body^1"
                            )
                    ));
                }

                bool.minimumShouldMatch("1");

                // Tenant isolation
                bool.filter(f -> f.term(t -> t
                        .field("tenantId")
                        .value(FieldValue.of(
                                TenantContext.get().toString()
                        ))
                ));

                // Visibility
                bool.filter(f -> f.terms(t -> t
                        .field("visibility")
                        .terms(values -> values.value(
                                allowedVisibilities.stream()
                                        .map(Visibility::name)
                                        .map(FieldValue::of)
                                        .toList()
                        ))
                ));

                // Optional category
                if (categoryId != null) {
                    bool.filter(f -> f.term(t -> t
                            .field("categoryId")
                            .value(FieldValue.of(
                                    categoryId.toString()
                            ))
                    ));
                }

                return bool;
            }));

            request.highlight(h -> h
                    .fields("body", f -> f
                            .fragmentSize(160)
                            .numberOfFragments(1)
                    )
                    .fields("title", f -> f
                            .numberOfFragments(0)
                    )
            );

            SearchResponse<ArticleSearchDocument> response =
                    client.search(
                            request.build(),
                            ArticleSearchDocument.class
                    );

            List<SearchResultResponse.SearchHit> hits = new ArrayList<>();

            for (Hit<ArticleSearchDocument> hit : response.hits().hits()) {
                ArticleSearchDocument doc = hit.source();

                if (doc == null) {
                    continue;
                }

                hits.add(new SearchResultResponse.SearchHit(
                        UUID.fromString(doc.articleId),
                        doc.slug,
                        doc.title,
                        excerptFrom(hit, doc),
                        doc.categoryId != null
                                ? UUID.fromString(doc.categoryId)
                                : null,
                        hit.score() != null
                                ? hit.score()
                                : 0.0
                ));
            }

            long total = response.hits().total() != null
                    ? response.hits().total().value()
                    : hits.size();

            result = new SearchResultResponse(
                    rawQuery,
                    hits,
                    total
            );

        } catch (Exception ex) {
            log.error(
                    "Search query failed for tenant {}",
                    TenantContext.getOrNull(),
                    ex
            );

            result = new SearchResultResponse(
                    rawQuery,
                    List.of(),
                    0
            );
        }

        recordSearchEvent(
                rawQuery,
                result,
                anonymousSessionId
        );

        return result;
    }

    private Map<String, Object> buildRequestBody(Set<String> expandedTerms, UUID categoryId, int page, int size,
                                                  Set<Visibility> allowedVisibilities) {
        List<Map<String, Object>> shouldClauses = new ArrayList<>();
        for (String term : expandedTerms) {
            shouldClauses.add(Map.of("multi_match", Map.of(
                    "query", term,
                    "fields", List.of("title^4", "headings^3", "keywords^2", "categoryName^1.5", "body^1"))));
        }

        List<Map<String, Object>> filters = new ArrayList<>();
        filters.add(Map.of("term", Map.of("tenantId", TenantContext.get().toString())));
        filters.add(Map.of("terms", Map.of("visibility",
                allowedVisibilities.stream().map(Enum::name).toList())));
        if (categoryId != null) {
            filters.add(Map.of("term", Map.of("categoryId", categoryId.toString())));
        }

        Map<String, Object> boolQuery = new LinkedHashMap<>();
        boolQuery.put("should", shouldClauses);
        boolQuery.put("minimum_should_match", 1);
        boolQuery.put("filter", filters);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("query", Map.of("bool", boolQuery));
        body.put("highlight", Map.of(
                "fields", Map.of(
                        "body", Map.of("fragment_size", 160, "number_of_fragments", 1),
                        "title", Map.of("number_of_fragments", 0))));
        body.put("from", page * size);
        body.put("size", size);
        body.put("track_total_hits", true);
        return body;
    }

    private String excerptFrom(Hit<ArticleSearchDocument> hit, ArticleSearchDocument doc) {
        Map<String, List<String>> highlights = hit.highlight();
        if (highlights != null && highlights.containsKey("body") && !highlights.get("body").isEmpty()) {
            return highlights.get("body").get(0);
        }
        if (doc.body != null && doc.body.length() > 160) {
            return doc.body.substring(0, 160) + "…";
        }
        return doc.body;
    }

    private void recordSearchEvent(String rawQuery, SearchResultResponse result, String anonymousSessionId) {
        boolean zeroResult = result.total() == 0;
        String metadata = "{\"query\":\"" + rawQuery.replace("\"", "") + "\",\"zeroResult\":" + zeroResult
                + ",\"resultCount\":" + result.total() + "}";
        // Full knowledge-gap scoring (spec section 17) aggregates these events in
        // Phase 5; this just guarantees the raw signal — including the zero-result
        // flag explicitly called out as Phase 4 scope — is captured from day one.
        analyticsEventService.record(AnalyticsEventType.SEARCH_PERFORMED, anonymousSessionId, null, null, null, metadata);
    }
}
