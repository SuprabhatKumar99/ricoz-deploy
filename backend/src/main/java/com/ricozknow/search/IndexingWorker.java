package com.ricozknow.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ricozknow.common.TenantContext;
import com.ricozknow.publishing.IndexJobPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Consumes the two queues IndexJobPublisher writes to:
 *
 *   ricozknow:index-jobs
 *   ricozknow:index-jobs:removals
 *
 * The workers are started only after the Spring application is fully ready.
 * This is important because LettuceConnectionFactory is managed by Spring's
 * lifecycle and may not yet be started during @PostConstruct.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IndexingWorker {

    private static final String INDEX_QUEUE_KEY = "ricozknow:index-jobs";
    private static final String REMOVAL_QUEUE_KEY = "ricozknow:index-jobs:removals";

    private static final Duration POLL_TIMEOUT = Duration.ofSeconds(5);

    private static final int MAX_ATTEMPTS = 3;

    private static final String DEAD_LETTER_QUEUE_KEY =
            "ricozknow:index-jobs:dead-letter";

    private final StringRedisTemplate redisTemplate;
    private final SearchIndexingService indexingService;
    private final ObjectMapper objectMapper;

    private final ExecutorService executor =
            Executors.newFixedThreadPool(2);

    private volatile boolean running = false;

    /**
     * Start Redis polling only after the complete Spring application
     * has finished initialization.
     *
     * Do NOT use @PostConstruct here because Redis's
     * LettuceConnectionFactory is a lifecycle-managed bean and may
     * still be stopped at that point.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void start() {

        if (running) {
            return;
        }

        running = true;

        log.info("Starting Redis indexing workers");

        executor.submit(() ->
                pollLoop(INDEX_QUEUE_KEY, this::handleIndexJob)
        );

        executor.submit(() ->
                pollLoop(REMOVAL_QUEUE_KEY, this::handleRemovalJob)
        );
    }

    /**
     * Stop workers cleanly when Spring shuts down.
     */
    @jakarta.annotation.PreDestroy
    public void stop() {

        log.info("Stopping Redis indexing workers");

        running = false;

        executor.shutdownNow();
    }

    private void pollLoop(
            String queueKey,
            java.util.function.Consumer<IndexJobPublisher.IndexJob> handler
    ) {

        log.info("Redis indexing worker started for queue {}", queueKey);

        while (running) {

            try {

                String payload =
                        redisTemplate
                                .opsForList()
                                .rightPop(queueKey, POLL_TIMEOUT);

                if (payload == null) {
                    continue;
                }

                processWithRetry(payload, handler);

            } catch (Exception ex) {

                if (running) {

                    log.error(
                            "Unexpected error in indexing worker poll loop for {}",
                            queueKey,
                            ex
                    );

                    /*
                     * Avoid a tight error loop if Redis temporarily becomes
                     * unavailable.
                     */
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }

        log.info("Redis indexing worker stopped for queue {}", queueKey);
    }

    private void processWithRetry(
            String payload,
            java.util.function.Consumer<IndexJobPublisher.IndexJob> handler
    ) {

        Exception last = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {

            try {

                IndexJobPublisher.IndexJob job =
                        objectMapper.readValue(
                                payload,
                                IndexJobPublisher.IndexJob.class
                        );

                TenantContext.set(job.tenantId());

                try {

                    handler.accept(job);

                    return;

                } finally {

                    TenantContext.clear();
                }

            } catch (Exception ex) {

                last = ex;

                log.warn(
                        "Index job attempt {}/{} failed",
                        attempt,
                        MAX_ATTEMPTS,
                        ex
                );

                if (attempt < MAX_ATTEMPTS) {

                    try {

                        Thread.sleep(250L * attempt);

                    } catch (InterruptedException interrupted) {

                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }

        log.error(
                "Index job failed after {} attempts; moving to dead-letter queue: {}",
                MAX_ATTEMPTS,
                payload,
                last
        );

        try {

            redisTemplate
                    .opsForList()
                    .leftPush(
                            DEAD_LETTER_QUEUE_KEY,
                            payload
                    );

        } catch (Exception dlqEx) {

            log.error(
                    "Unable to enqueue failed index job to dead-letter queue",
                    dlqEx
            );
        }
    }

    private void handleIndexJob(
            IndexJobPublisher.IndexJob job
    ) {

        indexingService.indexVersion(
                job.tenantId(),
                job.articleId(),
                job.versionId()
        );
    }

    private void handleRemovalJob(
            IndexJobPublisher.IndexJob job
    ) {

        indexingService.removeArticle(
                job.tenantId(),
                job.articleId()
        );
    }
}