package com.elotech.taskmanager.repository.usernotification;

import com.elotech.taskmanager.domain.dto.response.notification.NotificationResponse;
import com.elotech.taskmanager.domain.entity.UserNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserNotificationRepository extends JpaRepository<UserNotification, UserNotification.Id> {

    @Query("""
            select new com.elotech.taskmanager.domain.dto.response.notification.NotificationResponse(
                n.id,
                n.type,
                n.message,
                n.projectId,
                n.taskId,
                n.createdBy,
                n.createdAt,
                un.readAt,
                case when un.readAt is null then true else false end
            )
            from UserNotification un
            join Notification n on n.id = un.id.notificationId
            where un.id.userId = :userId
              and (
                :unread is null
                or (:unread = true and un.readAt is null)
                or (:unread = false and un.readAt is not null)
              )
            order by n.createdAt desc
            """)
    Page<NotificationResponse> findNotificationsForUser(
            @Param("userId") UUID userId,
            @Param("unread") Boolean unread,
            Pageable pageable
    );

    @Query("""
            select un from UserNotification un
            where un.id.userId = :userId
              and un.id.notificationId = :notificationId
            """)
    Optional<UserNotification> findByUserIdAndNotificationId(
            @Param("userId") UUID userId,
            @Param("notificationId") UUID notificationId
    );
}
