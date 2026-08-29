package com.elotech.taskmanager.repository.project;

import com.elotech.taskmanager.domain.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID>, JpaSpecificationExecutor<Project> {

    Optional<Project> findByIdAndDeletedAtIsNull(UUID projectId);

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
