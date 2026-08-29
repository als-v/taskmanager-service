package com.elotech.taskmanager.domain.criteria;

import com.elotech.taskmanager.domain.enumeration.Priority;
import com.elotech.taskmanager.domain.enumeration.TaskStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record TaskListCriteria(
        TaskStatus status,
        Priority priority,
        UUID assigneeId,
        LocalDateTime dueDateFrom,
        LocalDateTime dueDateTo,
        String title,
        String description,
        String sort,
        Integer page,
        Integer size,
        Boolean unassigned
) {
}
