package com.elotech.taskmanager.logging;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class HttpRequestContextExtractor {

    public RequestLogContext extract(HttpServletRequest request) {
        if (request == null) {
            return new RequestLogContext("unknown", "unknown", "unknown", "unknown", "unknown", "unknown", "unknown");
        }

        String userAgent = valueOrUnknown(request.getHeader("User-Agent"));

        return new RequestLogContext(
                resolveIp(request),
                request.getMethod(),
                request.getRequestURI(),
                userAgent,
                resolveDeviceType(userAgent),
                resolveBrowser(userAgent),
                resolveOs(userAgent)
        );
    }

    private String resolveIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");

        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");

        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }

    private String resolveDeviceType(String userAgent) {
        String normalized = userAgent.toLowerCase(Locale.ROOT);
        if (normalized.contains("tablet") || normalized.contains("ipad")) {
            return "tablet";
        }

        if (normalized.contains("mobi") || normalized.contains("android") || normalized.contains("iphone")) {
            return "mobile";
        }

        if (normalized.equals("unknown")) {
            return "unknown";
        }

        return "desktop";
    }

    private String resolveBrowser(String userAgent) {
        String normalized = userAgent.toLowerCase(Locale.ROOT);
        if (normalized.contains("edg/")) return "edge";
        if (normalized.contains("opr/") || normalized.contains("opera")) return "opera";
        if (normalized.contains("chrome/") || normalized.contains("crios/")) return "chrome";
        if (normalized.contains("firefox/") || normalized.contains("fxios/")) return "firefox";
        if (normalized.contains("safari/")) return "safari";
        return "unknown";
    }

    private String resolveOs(String userAgent) {
        String normalized = userAgent.toLowerCase(Locale.ROOT);
        if (normalized.contains("windows")) return "windows";
        if (normalized.contains("mac os") || normalized.contains("macintosh")) return "macos";
        if (normalized.contains("android")) return "android";
        if (normalized.contains("iphone") || normalized.contains("ipad") || normalized.contains("ios")) return "ios";
        if (normalized.contains("linux")) return "linux";
        return "unknown";
    }

    private String valueOrUnknown(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
