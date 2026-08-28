package com.elotech.taskmanager.repository;

import com.elotech.taskmanager.domain.entity.TaskLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TaskLogRepository extends JpaRepository<TaskLog, UUID> {
}
