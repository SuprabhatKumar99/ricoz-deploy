package com.ricozknow.publishing;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Spec section 15: publishing writes to PostgreSQL transactionally, then enqueues
 * an async indexing job. If OpenSearch/indexing fails, the publication is still
 * valid — the job just retries (or the index gets rebuilt from PostgreSQL).
 *
 * The actual indexing worker that consumes this queue is built in Phase 4;
 * this class only owns the "create index job" side of the handoff.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IndexJobPublisher {

    private static final String INDEX_QUEUE_KEY = "ricozknow:index-jobs";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void enqueueIndexJob(UUID tenantId, UUID articleId, UUID versionId) {
        try {
            IndexJob job = new IndexJob(tenantId, articleId, versionId, Instant.now());
            redisTemplate.opsForList().leftPush(INDEX_QUEUE_KEY, objectMapper.writeValueAsString(job));
        } catch (Exception ex) {
            // Never let indexing-job enqueue failures break publishing itself —
            // the article is already durably published in PostgreSQL at this point.
            log.error("Failed to enqueue index job for article {} version {}", articleId, versionId, ex);
        }
    }

    public void enqueueRemovalJob(UUID tenantId, UUID articleId) {
        try {
            IndexJob job = new IndexJob(tenantId, articleId, null, Instant.now());
            redisTemplate.opsForList().leftPush(INDEX_QUEUE_KEY + ":removals", objectMapper.writeValueAsString(job));
        } catch (Exception ex) {
            log.error("Failed to enqueue removal job for article {}", articleId, ex);
        }
    }

    public record IndexJob(UUID tenantId, UUID articleId, UUID versionId, Instant enqueuedAt) {
    }
}
