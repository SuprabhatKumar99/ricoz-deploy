-- Analytics and audit tables

CREATE TABLE analytics_events (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id             UUID NOT NULL REFERENCES tenants(id),
    event_type            VARCHAR(50) NOT NULL,
    anonymous_session_id  VARCHAR(100),
    user_id               UUID REFERENCES users(id),
    article_id            UUID REFERENCES articles(id),
    article_version_id    UUID REFERENCES article_versions(id),
    metadata              JSONB,
    occurred_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE audit_logs (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id      UUID NOT NULL REFERENCES tenants(id),
    actor_user_id  UUID REFERENCES users(id),
    entity_type    VARCHAR(100) NOT NULL,
    entity_id      UUID,
    action         VARCHAR(50) NOT NULL,
    old_state      JSONB,
    new_state      JSONB,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_analytics_events_tenant ON analytics_events(tenant_id);
CREATE INDEX idx_analytics_events_type ON analytics_events(tenant_id, event_type);
CREATE INDEX idx_analytics_events_occurred ON analytics_events(tenant_id, occurred_at);
CREATE INDEX idx_audit_logs_tenant ON audit_logs(tenant_id);
CREATE INDEX idx_audit_logs_entity ON audit_logs(tenant_id, entity_type, entity_id);

-- Row-Level Security scaffolding for high-risk tenant-owned tables.
-- Application sets `SET LOCAL app.current_tenant_id = '<uuid>'` per request/transaction.
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE articles ENABLE ROW LEVEL SECURITY;
ALTER TABLE article_versions ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_logs ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_users ON users
    USING (tenant_id = current_setting('app.current_tenant_id', true)::uuid);
CREATE POLICY tenant_isolation_articles ON articles
    USING (tenant_id = current_setting('app.current_tenant_id', true)::uuid);
CREATE POLICY tenant_isolation_article_versions ON article_versions
    USING (tenant_id = current_setting('app.current_tenant_id', true)::uuid);
CREATE POLICY tenant_isolation_audit_logs ON audit_logs
    USING (tenant_id = current_setting('app.current_tenant_id', true)::uuid);

-- NOTE: RLS is defense-in-depth on top of application-level tenant scoping,
-- not a replacement for it. The app must still filter by tenant_id in every query.
