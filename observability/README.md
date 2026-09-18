# RicozKnow Observability Stack

This directory provides a local observability stack for the backend:

- Prometheus: metrics storage and PromQL
- Loki: log storage
- Grafana: dashboards and exploration
- Tempo: distributed tracing
- OpenTelemetry Collector: OTLP gateway for metrics/traces
- Grafana Alloy: Docker log collection to Loki

## Start

From this directory:

```bash
docker compose up -d
```

Then open:

- Grafana: http://localhost:3000 (admin/admin by default; change credentials with env vars)
- Prometheus: http://localhost:9090
- Loki: http://localhost:3100/ready
- Tempo: http://localhost:3200/ready

## Backend configuration

When the backend runs on the host, the defaults in `application.yml` send OTLP to `localhost:4318`.
When the backend runs inside Docker, set:

```text
OTEL_METRICS_URL=http://otel-collector:4318/v1/metrics
OTEL_TRACING_ENDPOINT=http://otel-collector:4318/v1/traces
```

For a backend container outside this compose project, attach it to the `ricozknow-observability` Docker network or use the collector host address reachable from the container.

Prometheus also scrapes `/actuator/prometheus` from `host.docker.internal:8080` for host-based development.

## What is instrumented

- Spring Boot Actuator health/info/metrics/prometheus endpoints
- Micrometer Prometheus and OTLP registries
- Micrometer/OpenTelemetry tracing
- JSON application logs with MDC
- tenantId MDC correlation
- service-call latency/error metrics with low-cardinality service/operation/outcome tags
- OpenSearch health indicator
- JVM, HTTP server, datasource, Hikari, Redis and other Spring/Micrometer metrics where supported by the active libraries

Do not put raw tenant IDs, user IDs, article IDs, email addresses, JWTs, or request bodies into metric labels.
