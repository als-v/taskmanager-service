CREATE TABLE tasks (
    id UUID NOT NULL,
    project_id UUID NOT NULL,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(2000),
    status VARCHAR(20) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    user_id UUID,
    due_date TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_tasks PRIMARY KEY (id),

    CONSTRAINT fk_tasks_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_tasks_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_tasks_project_status ON tasks (project_id, status);
CREATE INDEX idx_tasks_project_user_status ON tasks (project_id, user_id, status);
CREATE INDEX idx_tasks_due_date ON tasks (due_date);
