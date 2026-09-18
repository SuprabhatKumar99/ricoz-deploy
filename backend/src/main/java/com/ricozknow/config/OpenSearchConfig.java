package com.ricozknow.config;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.core5.http.HttpHost;

import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;

import org.springframework.core.env.Environment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenSearch client configuration.
 *
 * Supports local OpenSearch and managed OpenSearch services such as Aiven.
 * Connection properties are read from Spring Environment, which allows
 * Render environment variables to be used without storing secrets in source.
 */
@Configuration
public class OpenSearchConfig {

    private final Environment environment;

    public OpenSearchConfig(Environment environment) {
        this.environment = environment;
    }

    @Bean
    public OpenSearchClient openSearchClient() {

        String host = environment.getProperty(
                "opensearch.host",
                "localhost"
        );

        int port = environment.getProperty(
                "opensearch.port",
                Integer.class,
                9200
        );

        String scheme = environment.getProperty(
                "opensearch.scheme",
                "http"
        );

        String username = environment.getProperty(
                "opensearch.username",
                ""
        );

        String password = environment.getProperty(
                "opensearch.password",
                ""
        );

        HttpHost httpHost = new HttpHost(
                scheme,
                host,
                port
        );

        ApacheHttpClient5TransportBuilder builder =
                ApacheHttpClient5TransportBuilder.builder(httpHost);

        /*
         * Configure username/password authentication when credentials
         * are provided. This is required for Aiven OpenSearch.
         */
        if (!username.isBlank() && !password.isBlank()) {

            BasicCredentialsProvider credentialsProvider =
                    new BasicCredentialsProvider();

            credentialsProvider.setCredentials(
                    new AuthScope(httpHost),
                    new UsernamePasswordCredentials(
                            username,
                            password.toCharArray()
                    )
            );

            builder.setHttpClientConfigCallback(
                    httpClientBuilder ->
                            httpClientBuilder
                                    .setDefaultCredentialsProvider(
                                            credentialsProvider
                                    )
            );
        }

        OpenSearchTransport transport = builder.build();

        return new OpenSearchClient(transport);
    }
}