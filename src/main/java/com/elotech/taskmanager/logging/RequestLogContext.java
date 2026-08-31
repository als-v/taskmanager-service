package com.elotech.taskmanager.logging;

public record RequestLogContext(
        String ip,
        String method,
        String path,
        String userAgent,
        String deviceType,
        String browser,
        String os
) {
}
