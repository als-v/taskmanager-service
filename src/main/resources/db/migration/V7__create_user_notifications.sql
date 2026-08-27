CREATE TABLE user_notifications (
    user_id UUID NOT NULL,
    notification_id UUID NOT NULL,
    read_at TIMESTAMP,

    CONSTRAINT pk_user_notifications PRIMARY KEY (user_id, notification_id),
    CONSTRAINT fk_user_notifications_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_notifications_notification FOREIGN KEY (notification_id) REFERENCES notifications (id)
);

CREATE INDEX idx_user_notifications_user_read ON user_notifications (user_id, read_at);
