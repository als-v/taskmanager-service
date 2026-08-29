package com.elotech.taskmanager.repository;

import com.elotech.taskmanager.domain.entity.Task;
import com.elotech.taskmanager.domain.enumeration.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;

import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    Page<Task> findAllByProjectIdAndDeletedAtIsNull(UUID projectId, Pageable pageable);

    long countByProjectIdAndUserIdAndStatusAndDeletedAtIsNull(UUID projectId, UUID userId, TaskStatus status);


    @Modifying
    @Query("update Task t set t.deletedAt = :deletedAt where t.projectId = :projectId and t.deletedAt is null")
    int updateDeletedAtByProjectIdAndDeletedAtIsNull(UUID projectId, LocalDateTime deletedAt);
}
