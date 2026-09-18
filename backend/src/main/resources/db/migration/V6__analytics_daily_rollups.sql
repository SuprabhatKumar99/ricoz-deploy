-- Daily rollups computed from analytics_events by the nightly aggregation job
-- (spec section 16: "Raw events are retained so aggregates can be
-- recalculated"). Each table is keyed so a rerun for the same tenant+date is
-- idempotent: the aggregation job deletes and re-inserts that date's rows
-- rather than incrementing counters in place.

CREATE TABLE analytics_daily_article_stats (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    article_id      UUID NOT NULL REFERENCES articles(id),
    stat_date       DATE NOT NULL,
    views           INT NOT NULL DEFAULT 0,
    helpful_count   INT NOT NULL DEFAULT 0,
    unhelpful_count INT NOT NULL DEFAULT 0,
    CONSTRAINT uq_daily_article_stats UNIQUE (tenant_id, article_id, stat_date)
);

CREATE TABLE analytics_daily_search_stats (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id),
    query_text          VARCHAR(500) NOT NULL,
    stat_date           DATE NOT NULL,
    volume_count        INT NOT NULL DEFAULT 0,
    zero_result_count   INT NOT NULL DEFAULT 0,
    unsuccessful_count  INT NOT NULL DEFAULT 0,
    CONSTRAINT uq_daily_search_stats UNIQUE (tenant_id, query_text, stat_date)
);

CREATE TABLE analytics_daily_deflection_stats (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id                   UUID NOT NULL REFERENCES tenants(id),
    stat_date                   DATE NOT NULL,
    estimated_deflection_count  INT NOT NULL DEFAULT 0,
    confirmed_deflection_count  INT NOT NULL DEFAULT 0,
    support_contact_count       INT NOT NULL DEFAULT 0,
    CONSTRAINT uq_daily_deflection_stats UNIQUE (tenant_id, stat_date)
);

CREATE INDEX idx_daily_article_stats_tenant_date ON analytics_daily_article_stats(tenant_id, stat_date);
CREATE INDEX idx_daily_search_stats_tenant_date ON analytics_daily_search_stats(tenant_id, stat_date);
CREATE INDEX idx_daily_deflection_stats_tenant_date ON analytics_daily_deflection_stats(tenant_id, stat_date);
