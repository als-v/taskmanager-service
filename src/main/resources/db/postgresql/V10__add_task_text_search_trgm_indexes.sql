CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_tasks_title_trgm_not_deleted
    ON tasks USING gin (lower(title) gin_trgm_ops)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_tasks_description_trgm_not_deleted
    ON tasks USING gin (lower(description) gin_trgm_ops)
    WHERE deleted_at IS NULL;
