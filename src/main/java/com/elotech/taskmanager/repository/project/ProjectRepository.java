package com.elotech.taskmanager.repository.project;

import com.elotech.taskmanager.domain.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    @Query("""
            select p
            from Project p
            join ProjectMember pm on pm.projectId = p.id
            where pm.userId = :userId
              and p.deletedAt is null
            """)
    Page<Project> findAllByMemberUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("""
            select p
            from Project p
            join ProjectMember pm on pm.projectId = p.id
            where p.id = :projectId
              and pm.userId = :userId
              and p.deletedAt is null
            """)
    Optional<Project> findByIdAndMemberUserId(@Param("projectId") UUID projectId, @Param("userId") UUID userId);
}
