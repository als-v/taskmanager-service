package com.elotech.taskmanager.repository.task;

import com.elotech.taskmanager.domain.entity.Task;
import com.elotech.taskmanager.domain.enumeration.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;

import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID>, JpaSpecificationExecutor<Task> {

    Page<Task> findAllByProjectIdAndDeletedAtIsNull(UUID projectId, Pageable pageable);

    long countByProjectIdAndUserIdAndStatusAndDeletedAtIsNull(UUID projectId, UUID userId, TaskStatus status);


    @Modifying
    @Query("update Task t set t.deletedAt = :deletedAt where t.projectId = :projectId and t.deletedAt is null")
    int updateDeletedAtByProjectIdAndDeletedAtIsNull(UUID projectId, LocalDateTime deletedAt);

    @Query("""
            select t.status as status, count(t.id) as total
            from Task t
            join ProjectMember pm on pm.projectId = t.projectId
            join Project p on p.id = t.projectId
            where pm.userId = :userId
              and t.deletedAt is null
              and p.deletedAt is null
            group by t.status
            """)
    List<TaskStatusCountProjection> countByStatusForMemberProjects(UUID userId);

    @Query("""
            select t.status as status, count(t.id) as total
            from Task t
            join ProjectMember pm on pm.projectId = t.projectId
            join Project p on p.id = t.projectId
            where pm.userId = :userId
              and t.projectId = :projectId
              and t.deletedAt is null
              and p.deletedAt is null
            group by t.status
            """)
    List<TaskStatusCountProjection> countByStatusForMemberProject(UUID userId, UUID projectId);

    @Query("""
            select t.priority as priority, count(t.id) as total
            from Task t
            join ProjectMember pm on pm.projectId = t.projectId
            join Project p on p.id = t.projectId
            where pm.userId = :userId
              and t.deletedAt is null
              and p.deletedAt is null
            group by t.priority
            """)
    List<TaskPriorityCountProjection> countByPriorityForMemberProjects(UUID userId);

    @Query("""
            select t.priority as priority, count(t.id) as total
            from Task t
            join ProjectMember pm on pm.projectId = t.projectId
            join Project p on p.id = t.projectId
            where pm.userId = :userId
              and t.projectId = :projectId
              and t.deletedAt is null
              and p.deletedAt is null
            group by t.priority
            """)
    List<TaskPriorityCountProjection> countByPriorityForMemberProject(UUID userId, UUID projectId);
}
