CREATE TABLE task_logs (
    id UUID NOT NULL,
    project_id UUID NOT NULL,
    task_id UUID NOT NULL,
    actor_id UUID NOT NULL,
    action VARCHAR(30) NOT NULL,
    from_status VARCHAR(20),
    to_status VARCHAR(20),
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_task_logs PRIMARY KEY (id),
    CONSTRAINT fk_task_logs_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_task_logs_actor FOREIGN KEY (actor_id) REFERENCES users (id)
);

CREATE INDEX idx_task_logs_task_created ON task_logs (task_id, created_at);
CREATE INDEX idx_task_logs_project_created ON task_logs (project_id, created_at);
