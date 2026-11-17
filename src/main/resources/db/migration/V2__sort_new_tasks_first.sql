ALTER TABLE task ADD COLUMN created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP;
CREATE INDEX task_project_creation_idx ON task(project_id, created_at DESC, id DESC);
