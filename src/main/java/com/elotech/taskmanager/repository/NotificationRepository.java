package com.elotech.taskmanager.repository;

import com.elotech.taskmanager.domain.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
}
