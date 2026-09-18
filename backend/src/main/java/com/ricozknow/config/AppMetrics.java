package com.ricozknow.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Spring Boot Actuator + Micrometer already auto-instrument HTTP request
 * latency/count, JVM memory/GC, DataSource pool usage, etc. — those cover
 * the generic "is the app healthy" picture. This class adds the handful of
 * business-specific counters that matter for THIS product's failure modes:
 * auth abuse, publishing throughput, and search quality (zero-result rate
 * is the earliest signal that content coverage is degrading).
 *
 * Exposed at /actuator/prometheus for scraping.
 */
@Component
public class AppMetrics {

    private final Counter loginSuccessCounter;
    private final Counter loginFailureCounter;
    private final Counter articlePublishedCounter;
    private final Counter searchZeroResultCounter;
    private final Counter searchPerformedCounter;
    private final Counter indexingFailureCounter;

    public AppMetrics(MeterRegistry registry) {
        this.loginSuccessCounter = Counter.builder("ricozknow.auth.login")
                .tag("outcome", "success").description("Successful logins").register(registry);
        this.loginFailureCounter = Counter.builder("ricozknow.auth.login")
                .tag("outcome", "failure").description("Failed login attempts").register(registry);
        this.articlePublishedCounter = Counter.builder("ricozknow.articles.published")
                .description("Articles published").register(registry);
        this.searchPerformedCounter = Counter.builder("ricozknow.search.performed")
                .description("Searches performed").register(registry);
        this.searchZeroResultCounter = Counter.builder("ricozknow.search.zero_result")
                .description("Searches that returned no results").register(registry);
        this.indexingFailureCounter = Counter.builder("ricozknow.search.indexing_failures")
                .description("Failed OpenSearch index/remove operations").register(registry);
    }

    public void recordLoginSuccess() { loginSuccessCounter.increment(); }
    public void recordLoginFailure() { loginFailureCounter.increment(); }
    public void recordArticlePublished() { articlePublishedCounter.increment(); }
    public void recordSearchPerformed(boolean zeroResult) {
        searchPerformedCounter.increment();
        if (zeroResult) {
            searchZeroResultCounter.increment();
        }
    }
    public void recordIndexingFailure() { indexingFailureCounter.increment(); }
}
