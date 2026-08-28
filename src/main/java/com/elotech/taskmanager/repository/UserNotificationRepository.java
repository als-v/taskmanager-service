package com.elotech.taskmanager.repository;

import com.elotech.taskmanager.domain.entity.UserNotification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserNotificationRepository extends JpaRepository<UserNotification, UserNotification.Id> {
}
