ALTER TABLE projects ADD COLUMN deleted_at TIMESTAMP NULL;
ALTER TABLE tasks ADD COLUMN deleted_at TIMESTAMP NULL;

CREATE INDEX idx_projects_deleted_at ON projects (deleted_at);
CREATE INDEX idx_tasks_project_deleted_at ON tasks (project_id, deleted_at);
