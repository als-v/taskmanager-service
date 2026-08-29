package com.elotech.taskmanager.repository;

import com.elotech.taskmanager.domain.dto.response.notification.NotificationResponse;
import com.elotech.taskmanager.domain.entity.Notification;
import com.elotech.taskmanager.domain.entity.User;
import com.elotech.taskmanager.domain.entity.UserNotification;
import com.elotech.taskmanager.domain.enumeration.NotificationType;
import com.elotech.taskmanager.repository.notification.NotificationRepository;
import com.elotech.taskmanager.repository.user.UserRepository;
import com.elotech.taskmanager.repository.usernotification.UserNotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class UserNotificationRepositoryTest {

    @Autowired private UserNotificationRepository userNotificationRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void findNotificationsForUserOrdersByNotificationCreatedAtAndFiltersUnread() {
        User user = userRepository.save(user("Usuario", "usuario@example.com"));
        Notification older = notificationRepository.save(notification(user.getId(), "Older"));
        Notification newer = notificationRepository.save(notification(user.getId(), "Newer"));
        setCreatedAt(older.getId(), LocalDateTime.of(2026, 1, 10, 9, 0));
        setCreatedAt(newer.getId(), LocalDateTime.of(2026, 1, 10, 10, 0));

        userNotificationRepository.save(UserNotification.builder()
                .id(new UserNotification.Id(user.getId(), older.getId()))
                .readAt(LocalDateTime.of(2026, 1, 10, 11, 0))
                .build());
        userNotificationRepository.save(UserNotification.builder()
                .id(new UserNotification.Id(user.getId(), newer.getId()))
                .readAt(null)
                .build());

        Page<NotificationResponse> all = userNotificationRepository.findNotificationsForUser(
                user.getId(), null, PageRequest.of(0, 20));
        Page<NotificationResponse> unread = userNotificationRepository.findNotificationsForUser(
                user.getId(), true, PageRequest.of(0, 20));

        assertThat(all.getContent())
                .extracting(NotificationResponse::message)
                .containsExactly("Newer", "Older");
        assertThat(unread.getContent())
                .singleElement()
                .satisfies(response -> {
                    assertThat(response.message()).isEqualTo("Newer");
                    assertThat(response.unread()).isTrue();
                    assertThat(response.readAt()).isNull();
                });
    }

    private Notification notification(UUID createdBy, String message) {
        return Notification.builder()
                .type(NotificationType.TASK_ASSIGNED)
                .message(message)
                .createdBy(createdBy)
                .taskId(UUID.randomUUID())
                .build();
    }

    private User user(String name, String email) {
        return User.builder()
                .name(name)
                .email(email)
                .password("hash")
                .build();
    }

    private void setCreatedAt(UUID notificationId, LocalDateTime createdAt) {
        jdbcTemplate.update(
                "update notifications set created_at = ? where id = ?",
                Timestamp.valueOf(createdAt),
                notificationId
        );
    }
}
