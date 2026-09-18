package com.ricozknow.observability;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Configuration
public class ObservabilityHealthConfig {

    @Bean("openSearch")
    HealthIndicator openSearchHealthIndicator() {
        return () -> {
            String host = System.getenv().getOrDefault("OPENSEARCH_HOST", "rz-opensearch");
            int port = Integer.parseInt(System.getenv().getOrDefault("OPENSEARCH_PORT", "9200"));
            String scheme = System.getenv().getOrDefault("OPENSEARCH_SCHEME", "http");
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(scheme + "://" + host + ":" + port + "/_cluster/health"))
                        .GET().build();
                var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.discarding());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return Health.up().withDetail("status", response.statusCode()).build();
                }
                return Health.down().withDetail("status", response.statusCode()).build();
            } catch (Exception ex) {
                return Health.down(ex).build();
            }
        };
    }
}
