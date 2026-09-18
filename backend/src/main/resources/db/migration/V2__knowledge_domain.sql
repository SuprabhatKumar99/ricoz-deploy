-- Knowledge domain tables (structure only; business logic lands in Phase 2)

CREATE TABLE categories (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL REFERENCES tenants(id),
    parent_id   UUID REFERENCES categories(id),
    name        VARCHAR(255) NOT NULL,
    slug        VARCHAR(255) NOT NULL,
    sort_order  INT NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_categories_tenant_slug UNIQUE (tenant_id, slug)
);

CREATE TABLE articles (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id),
    slug                VARCHAR(255) NOT NULL,
    title               VARCHAR(500) NOT NULL,
    status              VARCHAR(30)  NOT NULL DEFAULT 'DRAFT',
    visibility          VARCHAR(30)  NOT NULL DEFAULT 'PRIVATE',
    category_id         UUID REFERENCES categories(id),
    active_version_id   UUID,
    created_by          UUID NOT NULL REFERENCES users(id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_articles_tenant_slug UNIQUE (tenant_id, slug)
);

CREATE TABLE article_versions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id),
    article_id      UUID NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    version_number  INT NOT NULL,
    content         JSONB NOT NULL,
    change_summary  TEXT,
    status          VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    created_by      UUID NOT NULL REFERENCES users(id),
    reviewed_by     UUID REFERENCES users(id),
    reviewed_at     TIMESTAMPTZ,
    published_at    TIMESTAMPTZ,
    expires_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_article_versions_article_number UNIQUE (article_id, version_number)
);

ALTER TABLE articles
    ADD CONSTRAINT fk_articles_active_version
    FOREIGN KEY (active_version_id) REFERENCES article_versions(id);

CREATE TABLE search_synonyms (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL REFERENCES tenants(id),
    term        VARCHAR(255) NOT NULL,
    synonyms    JSONB NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_synonyms_tenant_term UNIQUE (tenant_id, term)
);

CREATE TABLE assets (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id    UUID NOT NULL REFERENCES tenants(id),
    article_id   UUID REFERENCES articles(id),
    storage_key  VARCHAR(1024) NOT NULL,
    filename     VARCHAR(500) NOT NULL,
    mime_type    VARCHAR(150) NOT NULL,
    size_bytes   BIGINT NOT NULL,
    created_by   UUID NOT NULL REFERENCES users(id),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_categories_tenant ON categories(tenant_id);
CREATE INDEX idx_articles_tenant ON articles(tenant_id);
CREATE INDEX idx_articles_tenant_status ON articles(tenant_id, status);
CREATE INDEX idx_article_versions_tenant ON article_versions(tenant_id);
CREATE INDEX idx_article_versions_article ON article_versions(article_id);
CREATE INDEX idx_assets_tenant ON assets(tenant_id);
