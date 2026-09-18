// package com.ricozknow.search;

// import com.fasterxml.jackson.databind.JsonNode;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.ricozknow.article.Article;
// import com.ricozknow.article.ArticleRepository;
// import com.ricozknow.article.ArticleVersion;
// import com.ricozknow.article.ArticleVersionRepository;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.opensearch.client.opensearch.OpenSearchClient;
// import org.opensearch.client.opensearch.core.DeleteRequest;
// import org.opensearch.client.opensearch.core.IndexRequest;
// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;

// import java.util.ArrayList;
// import java.util.List;
// import java.util.UUID;

// /**
//  * Only published versions are ever indexed (spec section 13). This class is
//  * intentionally the only place that translates Postgres rows into OpenSearch
//  * documents, so the indexing worker (async, off the publish request) and the
//  * admin-triggered reindex tool (Phase 4's "index rebuild tooling") share
//  * exactly one code path and can never drift from each other.
//  */
// @Service
// @RequiredArgsConstructor
// @Slf4j
// public class SearchIndexingService {

//     private final OpenSearchClient client;
//     private final ArticleRepository articleRepository;
//     private final ArticleVersionRepository versionRepository;
//     private final ObjectMapper objectMapper;

//     @Transactional(readOnly = true)
//     public void indexVersion(UUID tenantId, UUID articleId, UUID versionId) {
//         Article article = articleRepository.findByTenantIdAndId(tenantId, articleId).orElse(null);
//         ArticleVersion version = versionRepository.findByTenantIdAndId(tenantId, versionId).orElse(null);

//         if (article == null || version == null) {
//             log.warn("Skipping index job: article {} or version {} no longer exists", articleId, versionId);
//             return;
//         }
//         // Defensive re-check: only ever index what's actually published right now.
//         // A version can be superseded between enqueue and consume.
//         if (version.getStatus() != com.ricozknow.article.VersionStatus.PUBLISHED
//                 || !version.getId().equals(article.getActiveVersion() != null ? article.getActiveVersion().getId() : null)) {
//             log.info("Skipping index job for {}: version is no longer the active published version", articleId);
//             return;
//         }

//         ArticleSearchDocument doc = buildDocument(tenantId, article, version);
//         String docId = tenantId + ":" + article.getId(); // stable id: one doc per article, overwritten on republish

//         try {
//             client.index(IndexRequest.of(b -> b
//                     .index(OpenSearchIndexInitializer.INDEX_NAME)
//                     .id(docId)
//                     .document(doc)));
//         } catch (Exception ex) {
//             // Publication in Postgres already succeeded and is authoritative (spec
//             // section 23) — indexing failures are logged for the retry/rebuild path,
//             // never surfaced as a publishing failure.
//             log.error("Failed to index article {} version {}", articleId, versionId, ex);
//             throw new IndexingFailedException(ex);
//         }
//     }

//     @Transactional(readOnly = true)
//     public void removeArticle(UUID tenantId, UUID articleId) {
//         String docId = tenantId + ":" + articleId;
//         try {
//             client.delete(DeleteRequest.of(b -> b.index(OpenSearchIndexInitializer.INDEX_NAME).id(docId)));
//         } catch (Exception ex) {
//             log.error("Failed to remove article {} from index", articleId, ex);
//             throw new IndexingFailedException(ex);
//         }
//     }

//     private ArticleSearchDocument buildDocument(UUID tenantId, Article article, ArticleVersion version) {
//         List<String> headings = extractHeadings(version.getContent());
//         String body = extractBodyText(version.getContent());

//         return new ArticleSearchDocument(
//                 tenantId.toString(),
//                 article.getId().toString(),
//                 version.getId().toString(),
//                 article.getSlug(),
//                 article.getTitle(),
//                 headings,
//                 body,
//                 List.of(), // keyword tagging is a future (Could Have: AI-assisted) enhancement
//                 article.getCategory() != null ? article.getCategory().getId().toString() : null,
//                 article.getCategory() != null ? article.getCategory().getName() : null,
//                 article.getVisibility().name(),
//                 version.getPublishedAt() != null ? version.getPublishedAt().toString() : null
//         );
//     }

//     private List<String> extractHeadings(String contentJson) {
//         List<String> headings = new ArrayList<>();
//         forEachBlock(contentJson, block -> {
//             if ("heading".equals(textOrNull(block, "type")) && textOrNull(block, "text") != null) {
//                 headings.add(textOrNull(block, "text"));
//             }
//         });
//         return headings;
//     }

//     private String extractBodyText(String contentJson) {
//         StringBuilder body = new StringBuilder();
//         forEachBlock(contentJson, block -> {
//             String type = textOrNull(block, "type");
//             String text = textOrNull(block, "text");
//             if (text != null && ("paragraph".equals(type) || "code".equals(type))) {
//                 body.append(text).append(' ');
//             }
//         });
//         return body.toString().trim();
//     }

//     private void forEachBlock(String contentJson, java.util.function.Consumer<JsonNode> consumer) {
//         try {
//             JsonNode root = objectMapper.readTree(contentJson);
//             JsonNode blocks = root.get("blocks");
//             if (blocks != null && blocks.isArray()) {
//                 blocks.forEach(consumer);
//             }
//         } catch (Exception ex) {
//             log.warn("Failed to parse article content while indexing; indexing with empty body", ex);
//         }
//     }

//     private String textOrNull(JsonNode node, String field) {
//         JsonNode value = node.get(field);
//         return value != null ? value.asText() : null;
//     }

//     public static class IndexingFailedException extends RuntimeException {
//         public IndexingFailedException(Throwable cause) {
//             super(cause);
//         }
//     }
// }


package com.ricozknow.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.ricozknow.article.Article;
import com.ricozknow.article.ArticleRepository;
import com.ricozknow.article.ArticleVersion;
import com.ricozknow.article.ArticleVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.DeleteRequest;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchIndexingService {

    private final OpenSearchClient client;
    private final ArticleRepository articleRepository;
    private final ArticleVersionRepository versionRepository;

    @Transactional(readOnly = true)
    public void indexVersion(
            UUID tenantId,
            UUID articleId,
            UUID versionId) {

        Article article = articleRepository
                .findByTenantIdAndId(tenantId, articleId)
                .orElse(null);

        ArticleVersion version = versionRepository
                .findByTenantIdAndId(tenantId, versionId)
                .orElse(null);

        if (article == null || version == null) {
            log.warn(
                    "Skipping index job: article {} or version {} no longer exists",
                    articleId,
                    versionId
            );
            return;
        }

        if (version.getStatus() != com.ricozknow.article.VersionStatus.PUBLISHED
                || !version.getId().equals(
                article.getActiveVersion() != null
                        ? article.getActiveVersion().getId()
                        : null)) {

            log.info(
                    "Skipping index job for {}: version is no longer the active published version",
                    articleId
            );
            return;
        }

        ArticleSearchDocument doc =
                buildDocument(tenantId, article, version);

        String docId =
                tenantId + ":" + article.getId();

        try {

            client.index(IndexRequest.of(b -> b
                    .index(OpenSearchIndexInitializer.INDEX_NAME)
                    .id(docId)
                    .document(doc)
            ));

        } catch (Exception ex) {

            log.error(
                    "Failed to index article {} version {}",
                    articleId,
                    versionId,
                    ex
            );

            throw new IndexingFailedException(ex);
        }
    }

    @Transactional(readOnly = true)
    public void removeArticle(
            UUID tenantId,
            UUID articleId) {

        String docId =
                tenantId + ":" + articleId;

        try {

            client.delete(
                    DeleteRequest.of(b -> b
                            .index(OpenSearchIndexInitializer.INDEX_NAME)
                            .id(docId)
                    )
            );

        } catch (Exception ex) {

            log.error(
                    "Failed to remove article {} from index",
                    articleId,
                    ex
            );

            throw new IndexingFailedException(ex);
        }
    }

    private ArticleSearchDocument buildDocument(
            UUID tenantId,
            Article article,
            ArticleVersion version) {

        List<String> headings =
                extractHeadings(version.getContent());

        String body =
                extractBodyText(version.getContent());

        return new ArticleSearchDocument(
                tenantId.toString(),
                article.getId().toString(),
                version.getId().toString(),
                article.getSlug(),
                article.getTitle(),
                headings,
                body,
                List.of(),
                article.getCategory() != null
                        ? article.getCategory().getId().toString()
                        : null,
                article.getCategory() != null
                        ? article.getCategory().getName()
                        : null,
                article.getVisibility().name(),
                version.getPublishedAt() != null
                        ? version.getPublishedAt().toString()
                        : null
        );
    }

    private List<String> extractHeadings(JsonNode content) {

        List<String> headings = new ArrayList<>();

        forEachBlock(content, block -> {

            String type =
                    textOrNull(block, "type");

            String text =
                    textOrNull(block, "text");

            if ("heading".equals(type) && text != null) {
                headings.add(text);
            }
        });

        return headings;
    }

    private String extractBodyText(JsonNode content) {

        StringBuilder body =
                new StringBuilder();

        forEachBlock(content, block -> {

            String type =
                    textOrNull(block, "type");

            String text =
                    textOrNull(block, "text");

            if (text != null
                    && ("paragraph".equals(type)
                    || "code".equals(type))) {

                body.append(text).append(' ');
            }
        });

        return body.toString().trim();
    }

    private void forEachBlock(
            JsonNode content,
            Consumer<JsonNode> consumer) {

        if (content == null || !content.isObject()) {
            return;
        }

        JsonNode blocks =
                content.get("blocks");

        if (blocks != null && blocks.isArray()) {
            blocks.forEach(consumer);
        }
    }

    private String textOrNull(
            JsonNode node,
            String field) {

        JsonNode value =
                node.get(field);

        return value != null && value.isValueNode()
                ? value.asText()
                : null;
    }

    public static class IndexingFailedException
            extends RuntimeException {

        public IndexingFailedException(Throwable cause) {
            super(cause);
        }
    }
}