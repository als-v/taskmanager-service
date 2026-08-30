package com.elotech.taskmanager.repository.task;

import com.elotech.taskmanager.domain.enumeration.Priority;

public interface TaskPriorityCountProjection {

    Priority getPriority();

    long getTotal();
}
