package com.ricozknow.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ricozknow.common.TenantContext;
import com.ricozknow.publishing.IndexJobPublisher;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Consumes the two queues IndexJobPublisher writes to:
 *   ricozknow:index-jobs           -> newly published versions to index
 *   ricozknow:index-jobs:removals  -> archived articles to remove from the index
 *
 * Runs as a dedicated blocking-poll thread rather than @Scheduled, since a
 * blocking Redis BRPOP-style read (rightPop with timeout) is a cleaner fit
 * than a tight scheduled poll loop. Every job is retried on failure per spec
 * section 20 ("All jobs must be idempotent, retryable"); indexing itself is
 * naturally idempotent since documents are upserted by a stable id.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IndexingWorker {

    private static final String INDEX_QUEUE_KEY = "ricozknow:index-jobs";
    private static final String REMOVAL_QUEUE_KEY = "ricozknow:index-jobs:removals";
    private static final Duration POLL_TIMEOUT = Duration.ofSeconds(5);

    private final StringRedisTemplate redisTemplate;
    private final SearchIndexingService indexingService;
    private final ObjectMapper objectMapper;

    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private volatile boolean running = true;

    @PostConstruct
    public void start() {
        executor.submit(() -> pollLoop(INDEX_QUEUE_KEY, this::handleIndexJob));
        executor.submit(() -> pollLoop(REMOVAL_QUEUE_KEY, this::handleRemovalJob));
    }

    @PreDestroy
    public void stop() {
        running = false;
        executor.shutdownNow();
    }

    private void pollLoop(String queueKey, java.util.function.Consumer<IndexJobPublisher.IndexJob> handler) {
        while (running) {
            try {
                String payload = redisTemplate.opsForList().rightPop(queueKey, POLL_TIMEOUT);
                if (payload == null) {
                    continue; // timed out waiting; loop again
                }
                processWithRetry(payload, handler);
            } catch (Exception ex) {
                if (running) {
                    log.error("Unexpected error in indexing worker poll loop for {}", queueKey, ex);
                }
            }
        }
    }

    private static final int MAX_ATTEMPTS = 3;
    private static final String DEAD_LETTER_QUEUE_KEY = "ricozknow:index-jobs:dead-letter";

    private void processWithRetry(String payload, java.util.function.Consumer<IndexJobPublisher.IndexJob> handler) {
        Exception last = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                IndexJobPublisher.IndexJob job = objectMapper.readValue(payload, IndexJobPublisher.IndexJob.class);
                TenantContext.set(job.tenantId());
                try {
                    handler.accept(job);
                    return;
                } finally {
                    TenantContext.clear();
                }
            } catch (Exception ex) {
                last = ex;
                if (attempt < MAX_ATTEMPTS) {
                    try { Thread.sleep(250L * attempt); }
                    catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); return; }
                }
            }
        }
        log.error("Index job failed after {} attempts; moving to dead-letter queue: {}", MAX_ATTEMPTS, payload, last);
        try { redisTemplate.opsForList().leftPush(DEAD_LETTER_QUEUE_KEY, payload); }
        catch (Exception dlqEx) { log.error("Unable to enqueue failed index job to dead-letter queue", dlqEx); }
    }

    private void handleIndexJob(IndexJobPublisher.IndexJob job) {
        indexingService.indexVersion(job.tenantId(), job.articleId(), job.versionId());
    }

    private void handleRemovalJob(IndexJobPublisher.IndexJob job) {
        indexingService.removeArticle(job.tenantId(), job.articleId());
    }
}
