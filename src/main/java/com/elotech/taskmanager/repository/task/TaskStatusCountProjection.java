package com.elotech.taskmanager.repository.task;

import com.elotech.taskmanager.domain.enumeration.TaskStatus;

public interface TaskStatusCountProjection {

    TaskStatus getStatus();

    long getTotal();
}
