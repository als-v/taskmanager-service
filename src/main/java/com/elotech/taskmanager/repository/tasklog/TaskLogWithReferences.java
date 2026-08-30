package com.elotech.taskmanager.repository.tasklog;

import com.elotech.taskmanager.domain.entity.Task;
import com.elotech.taskmanager.domain.entity.TaskLog;
import com.elotech.taskmanager.domain.entity.User;

public record TaskLogWithReferences(TaskLog log, Task task, User actor) {
}
