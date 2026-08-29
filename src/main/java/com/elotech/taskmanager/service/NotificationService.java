package com.elotech.taskmanager.service;

import com.elotech.taskmanager.domain.dto.response.common.PageResponse;
import com.elotech.taskmanager.domain.dto.response.notification.NotificationResponse;
import com.elotech.taskmanager.domain.entity.Notification;
import com.elotech.taskmanager.domain.entity.User;
import com.elotech.taskmanager.domain.entity.UserNotification;
import com.elotech.taskmanager.domain.error.BadRequestException;
import com.elotech.taskmanager.domain.error.ErrorMessages;
import com.elotech.taskmanager.domain.error.NotFoundException;
import com.elotech.taskmanager.repository.NotificationRepository;
import com.elotech.taskmanager.repository.UserNotificationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class NotificationService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final UserNotificationRepository userNotificationRepository;
    private final NotificationRepository notificationRepository;
    private final CurrentUserService currentUserService;

    public NotificationService(UserNotificationRepository userNotificationRepository, NotificationRepository notificationRepository, CurrentUserService currentUserService) {
        this.userNotificationRepository = userNotificationRepository;
        this.notificationRepository = notificationRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(Boolean unread, Integer page, Integer size) {
        User currentUser = currentUserService.getCurrentUser();
        PageRequest pageRequest = pageRequest(page, size);

        return PageResponse.from(userNotificationRepository.findNotificationsForUser(currentUser.getId(), unread, pageRequest));
    }

    @Transactional
    public NotificationResponse markAsRead(UUID notificationId) {
        User currentUser = currentUserService.getCurrentUser();
        UserNotification userNotification = userNotificationRepository.findByUserIdAndNotificationId(currentUser.getId(), notificationId).orElseThrow(() -> new NotFoundException(ErrorMessages.NOTIFICATION_NOT_FOUND_CODE, ErrorMessages.NOTIFICATION_NOT_FOUND_MESSAGE));

        if (userNotification.getReadAt() == null) {
            userNotification.setReadAt(LocalDateTime.now());
            userNotificationRepository.save(userNotification);
        }

        Notification notification = notificationRepository.findById(notificationId).orElseThrow(() -> new NotFoundException(ErrorMessages.NOTIFICATION_NOT_FOUND_CODE, ErrorMessages.NOTIFICATION_NOT_FOUND_MESSAGE));
        return NotificationResponse.from(notification, userNotification.getReadAt());
    }

    private PageRequest pageRequest(Integer page, Integer size) {
        int pageValue = page == null ? DEFAULT_PAGE : page;
        int sizeValue = size == null ? DEFAULT_SIZE : size;

        if (pageValue < 0) {
            throw new BadRequestException(ErrorMessages.PAGINATION_PAGE_INVALID_CODE, ErrorMessages.PAGINATION_PAGE_INVALID_MESSAGE);
        }

        if (sizeValue < 1) {
            throw new BadRequestException(ErrorMessages.PAGINATION_SIZE_INVALID_CODE, ErrorMessages.PAGINATION_SIZE_INVALID_MESSAGE);
        }

        return PageRequest.of(pageValue, Math.min(sizeValue, MAX_SIZE));
    }
}
