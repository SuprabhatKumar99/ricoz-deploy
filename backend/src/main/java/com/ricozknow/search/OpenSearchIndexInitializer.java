package com.ricozknow.search;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.ExistsRequest;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;


/**
 * Single shared "articles" index across all tenants, isolated by the
 * tenant_id keyword field on every document and filtered on every query
 * (spec section 5: "All OpenSearch documents also contain tenant_id").
 *
 * Field weighting from spec section 12 (title > headings > keyword > body >
 * category) is applied at query time via boosts in SearchService, not baked
 * into the mapping itself.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OpenSearchIndexInitializer implements ApplicationRunner {

    public static final String INDEX_NAME = "articles";



    private final OpenSearchClient client;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        boolean exists = client.indices().exists(ExistsRequest.of(e -> e.index(INDEX_NAME))).value();
        if (exists) {
            return;
        }

        client.indices().create(CreateIndexRequest.of(b -> b
                .index(INDEX_NAME)
                .mappings(m -> m
                        .properties("tenantId", p -> p.keyword(k -> k))
                        .properties("articleId", p -> p.keyword(k -> k))
                        .properties("versionId", p -> p.keyword(k -> k))
                        .properties("slug", p -> p.keyword(k -> k))
                        .properties("title", p -> p.text(t -> t.analyzer("standard")))
                        .properties("headings", p -> p.text(t -> t.analyzer("standard")))
                        .properties("body", p -> p.text(t -> t.analyzer("standard")))
                        .properties("keywords", p -> p.text(t -> t.analyzer("standard")))
                        .properties("categoryId", p -> p.keyword(k -> k))
                        .properties("categoryName", p -> p.text(t -> t.analyzer("standard")))
                        .properties("visibility", p -> p.keyword(k -> k))
                        .properties("publishedAt", p -> p.date(d -> d))
                )));
        log.info("Created OpenSearch index '{}'", INDEX_NAME);
    }
}
