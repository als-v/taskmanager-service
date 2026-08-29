package com.elotech.taskmanager.repository.tasklog;

import com.elotech.taskmanager.domain.entity.TaskLog;
import com.elotech.taskmanager.domain.enumeration.AuditAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface TaskLogRepository extends JpaRepository<TaskLog, UUID> {

    @Query("""
            select tl from TaskLog tl
            where tl.projectId = :projectId
              and (:taskId is null or tl.taskId = :taskId)
              and (:actorId is null or tl.actorId = :actorId)
              and (:action is null or tl.action = :action)
            """)
    Page<TaskLog> findByProjectIdWithFilters(
            @Param("projectId") UUID projectId,
            @Param("taskId") UUID taskId,
            @Param("actorId") UUID actorId,
            @Param("action") AuditAction action,
            Pageable pageable
    );
}
