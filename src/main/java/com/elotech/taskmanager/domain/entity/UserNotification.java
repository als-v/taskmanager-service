package com.elotech.taskmanager.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_notifications", indexes = {
    @Index(name = "idx_user_notifications_user_read", columnList = "user_id, read_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserNotification {

    @Embeddable
    @Getter
    @Setter
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Id implements Serializable {

        @Column(name = "user_id", nullable = false)
        private UUID userId;

        @Column(name = "notification_id", nullable = false)
        private UUID notificationId;
    }

    @EmbeddedId
    private Id id;

    @Column(name = "read_at")
    private LocalDateTime readAt;
}
