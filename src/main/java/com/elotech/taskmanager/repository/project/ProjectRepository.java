package com.elotech.taskmanager.repository.project;

import com.elotech.taskmanager.domain.dto.response.dashboard.DashboardProjectResponse;
import com.elotech.taskmanager.domain.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;

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

    @Query("""
            select new com.elotech.taskmanager.domain.dto.response.dashboard.DashboardProjectResponse(p.id, p.name)
            from Project p
            join ProjectMember pm on pm.projectId = p.id
            where pm.userId = :userId
              and p.deletedAt is null
            order by p.name asc
            """)
    List<DashboardProjectResponse> findDashboardProjectsByMemberUserId(@Param("userId") UUID userId);
}
