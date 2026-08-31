package com.elotech.taskmanager.logging;

import com.elotech.taskmanager.domain.enumeration.AuditAction;
import com.elotech.taskmanager.domain.enumeration.TaskStatus;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Component
public class ApplicationEventLogger {

    private static final Logger eventLog = LoggerFactory.getLogger("taskmanager.events");

    private final HttpRequestContextExtractor requestContextExtractor;

    public ApplicationEventLogger(HttpRequestContextExtractor requestContextExtractor) {
        this.requestContextExtractor = requestContextExtractor;
    }

    public void auth(String eventName, String result, UUID userId, String email) {
        auth(currentRequest(), eventName, result, userId, email);
    }

    public void auth(HttpServletRequest request, String eventName, String result, UUID userId, String email) {
        RequestLogContext context = requestContextExtractor.extract(request);
        eventLog.info("auth_event {} {} {} {} {} {} {} {} {} {} {} {}",
                kv("event_type", "auth"),
                kv("event_name", eventName),
                kv("result", result),
                kv("user_id", userId),
                kv("email", email),
                kv("ip", context.ip()),
                kv("user_agent", context.userAgent()),
                kv("device_type", context.deviceType()),
                kv("browser", context.browser()),
                kv("os", context.os()),
                kv("method", context.method()),
                kv("path", context.path())
        );
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getRequest();
    }

    public void audit(
            AuditAction action,
            UUID projectId,
            UUID taskId,
            UUID actorId,
            TaskStatus fromStatus,
            TaskStatus toStatus
    ) {
        eventLog.info("audit_event {} {} {} {} {} {} {} {}",
                kv("event_type", "audit"),
                kv("action", action),
                kv("project_id", projectId),
                kv("task_id", taskId),
                kv("actor_id", actorId),
                kv("from_status", fromStatus),
                kv("to_status", toStatus),
                kv("result", "success")
        );
    }
}
