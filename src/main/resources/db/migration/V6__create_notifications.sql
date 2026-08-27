CREATE TABLE notifications (
    id UUID NOT NULL,
    type VARCHAR(30)  NOT NULL,
    message VARCHAR(1000) NOT NULL,
    project_id UUID,
    task_id UUID,
    created_by UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT pk_notifications PRIMARY KEY (id),
    CONSTRAINT fk_notifications_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_notifications_task FOREIGN KEY (task_id) REFERENCES tasks (id),
    CONSTRAINT fk_notifications_created_by FOREIGN KEY (created_by) REFERENCES users (id)
);

CREATE INDEX idx_notifications_created_at ON notifications (created_at);
