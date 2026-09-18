-- Fixes a schema/entity mismatch: TenantOwnedEntity (used by Role, ArticleVersion,
-- Asset) stamps updated_at on every write via @UpdateTimestamp, but these three
-- tables were created without that column in V1/V2. Roles rarely change, but
-- article_versions and assets genuinely do get mutated post-insert (review
-- fields, reassignment), so tracking updated_at on them is correct, not just
-- a workaround.

ALTER TABLE roles           ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT now();
ALTER TABLE roles ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT now();
ALTER TABLE article_versions ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT now();
ALTER TABLE assets          ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT now();
