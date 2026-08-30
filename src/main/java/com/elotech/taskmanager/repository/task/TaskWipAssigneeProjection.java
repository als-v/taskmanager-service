package com.elotech.taskmanager.repository.task;

import java.util.UUID;

public interface TaskWipAssigneeProjection {

    UUID getUserId();

    String getName();

    String getEmail();

    long getInProgress();
}
