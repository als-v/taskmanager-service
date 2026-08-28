package com.elotech.taskmanager.repository;

import com.elotech.taskmanager.domain.entity.Task;
import com.elotech.taskmanager.domain.enumeration.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    Page<Task> findAllByProjectId(UUID projectId, Pageable pageable);

    long countByProjectIdAndUserIdAndStatus(UUID projectId, UUID userId, TaskStatus status);
}
