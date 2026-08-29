package com.elotech.taskmanager.service;

import com.elotech.taskmanager.domain.dto.response.common.PageResponse;
import com.elotech.taskmanager.domain.dto.response.notification.NotificationResponse;
import com.elotech.taskmanager.domain.entity.Notification;
import com.elotech.taskmanager.domain.entity.User;
import com.elotech.taskmanager.domain.entity.UserNotification;
import com.elotech.taskmanager.domain.enumeration.NotificationType;
import com.elotech.taskmanager.domain.error.BadRequestException;
import com.elotech.taskmanager.domain.error.NotFoundException;
import com.elotech.taskmanager.repository.NotificationRepository;
import com.elotech.taskmanager.repository.UserNotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private UserNotificationRepository userNotificationRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private CurrentUserService currentUserService;

    private NotificationService notificationService;
    private User currentUser;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(userNotificationRepository, notificationRepository, currentUserService);
        currentUser = User.builder().id(UUID.randomUUID()).name("Maria").email("maria@example.com").build();
    }

    @Test
    void listsNotificationsForCurrentUser() {
        NotificationResponse notification = response(null);
        given(currentUserService.getCurrentUser()).willReturn(currentUser);
        given(userNotificationRepository.findNotificationsForUser(eq(currentUser.getId()), eq(null), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(notification)));

        PageResponse<NotificationResponse> response = notificationService.list(null, null, null);

        assertThat(response.content()).containsExactly(notification);
        assertThat(response.content().get(0).unread()).isTrue();
    }

    @Test
    void filtersUnreadAndCapsPageSize() {
        given(currentUserService.getCurrentUser()).willReturn(currentUser);
        given(userNotificationRepository.findNotificationsForUser(eq(currentUser.getId()), eq(true), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        notificationService.list(true, 1, 200);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userNotificationRepository).findNotificationsForUser(eq(currentUser.getId()), eq(true), pageableCaptor.capture());

        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(100);
        assertThat(pageableCaptor.getValue().getSort().isUnsorted()).isTrue();
    }

    @Test
    void marksNotificationAsRead() {
        UUID notificationId = UUID.randomUUID();
        UserNotification userNotification = userNotification(notificationId, null);
        Notification notification = notification(notificationId);

        given(currentUserService.getCurrentUser()).willReturn(currentUser);
        given(userNotificationRepository.findByUserIdAndNotificationId(currentUser.getId(), notificationId))
                .willReturn(Optional.of(userNotification));
        given(notificationRepository.findById(notificationId)).willReturn(Optional.of(notification));

        NotificationResponse response = notificationService.markAsRead(notificationId);

        assertThat(response.readAt()).isNotNull();
        assertThat(response.unread()).isFalse();
        verify(userNotificationRepository).save(userNotification);
    }

    @Test
    void markAsReadIsIdempotent() {
        UUID notificationId = UUID.randomUUID();
        LocalDateTime readAt = LocalDateTime.of(2026, 1, 10, 9, 0);
        UserNotification userNotification = userNotification(notificationId, readAt);
        Notification notification = notification(notificationId);

        given(currentUserService.getCurrentUser()).willReturn(currentUser);
        given(userNotificationRepository.findByUserIdAndNotificationId(currentUser.getId(), notificationId))
                .willReturn(Optional.of(userNotification));
        given(notificationRepository.findById(notificationId)).willReturn(Optional.of(notification));

        NotificationResponse response = notificationService.markAsRead(notificationId);

        assertThat(response.readAt()).isEqualTo(readAt);
        assertThat(response.unread()).isFalse();

        verify(userNotificationRepository, never()).save(any(UserNotification.class));
    }

    @Test
    void rejectsInvalidPagination() {
        assertThatThrownBy(() -> notificationService.list(null, -1, 20)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> notificationService.list(null, 0, 0)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void returnsNotFoundWhenNotificationDoesNotBelongToUser() {
        UUID notificationId = UUID.randomUUID();
        given(currentUserService.getCurrentUser()).willReturn(currentUser);
        given(userNotificationRepository.findByUserIdAndNotificationId(currentUser.getId(), notificationId))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(notificationId)).isInstanceOf(NotFoundException.class);
    }

    private NotificationResponse response(LocalDateTime readAt) {
        return NotificationResponse.from(notification(UUID.randomUUID()), readAt);
    }

    private Notification notification(UUID notificationId) {
        return Notification.builder()
                .id(notificationId)
                .type(NotificationType.TASK_ASSIGNED)
                .message("Voce foi atribuido a uma task")
                .projectId(UUID.randomUUID())
                .taskId(UUID.randomUUID())
                .createdBy(UUID.randomUUID())
                .createdAt(LocalDateTime.of(2026, 1, 10, 9, 0))
                .build();
    }

    private UserNotification userNotification(UUID notificationId, LocalDateTime readAt) {
        return UserNotification.builder()
                .id(new UserNotification.Id(currentUser.getId(), notificationId))
                .readAt(readAt)
                .build();
    }
}
