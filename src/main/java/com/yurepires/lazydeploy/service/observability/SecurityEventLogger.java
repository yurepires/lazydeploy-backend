package com.yurepires.lazydeploy.service.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.EnumSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** Centraliza eventos pesquisáveis sem registrar identidade ou segredos. */
@Component
public class SecurityEventLogger {

    private static final Logger log = LoggerFactory.getLogger(SecurityEventLogger.class);
    private static final Set<String> HTTP_METHODS = Set.of(
            "GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
    );
    private static final Set<SecurityEventType> HIGH_VOLUME_EVENTS = EnumSet.of(
            SecurityEventType.AUTH_LOGIN_FAILURE,
            SecurityEventType.AUTH_RATE_LIMITED,
            SecurityEventType.REGISTER_RATE_LIMITED,
            SecurityEventType.RATE_LIMIT_REJECTED,
            SecurityEventType.CSRF_REJECTED,
            SecurityEventType.CORS_REJECTED,
            SecurityEventType.RESOURCE_SATURATION,
            SecurityEventType.UNAUTHORIZED_REQUEST,
            SecurityEventType.ACCESS_DENIED,
            SecurityEventType.RESOURCE_NOT_OWNED,
            SecurityEventType.BUSINESS_LIMIT_REJECTED,
            SecurityEventType.PROVIDER_FAILURE
    );
    private static final long HIGH_VOLUME_LOG_INTERVAL_NANOS = 10_000_000_000L;

    private final LogSanitizer sanitizer;
    private final ConcurrentHashMap<SecurityEventType, AtomicLong> lastLoggedAt =
            new ConcurrentHashMap<>();

    public SecurityEventLogger(LogSanitizer sanitizer) {
        this.sanitizer = sanitizer;
    }

    public void log(
            SecurityEventType eventType,
            SecurityEventOutcome outcome,
            String reason,
            String endpoint
    ) {
        log(eventType, outcome, reason, endpoint, null);
    }

    public void log(
            SecurityEventType eventType,
            SecurityEventOutcome outcome,
            String reason,
            String endpoint,
            String method
    ) {
        String normalizedReason = sanitizer.sanitize(reason).toUpperCase(Locale.ROOT);
        String normalizedEndpoint = sanitizer.normalizeEndpoint(endpoint);
        String normalizedMethod = normalizeMethod(method);
        String correlationId = MDC.get("correlationId");

        if (!shouldLog(eventType)) {
            return;
        }

        if (isWarningEvent(eventType, outcome)) {
            log.warn(
                    "SECURITY EVENT | eventType={} | outcome={} | reason={} | endpoint={} | method={} | correlationId={} | timestamp={}",
                    eventType,
                    outcome,
                    normalizedReason,
                    normalizedEndpoint,
                    normalizedMethod,
                    correlationId,
                    Instant.now()
            );
            return;
        }

        log.info(
                "SECURITY EVENT | eventType={} | outcome={} | reason={} | endpoint={} | method={} | correlationId={} | timestamp={}",
                eventType,
                outcome,
                normalizedReason,
                normalizedEndpoint,
                normalizedMethod,
                correlationId,
                Instant.now()
        );
    }

    private String normalizeMethod(String method) {
        if (method == null || method.isBlank()) {
            return "UNKNOWN";
        }
        String normalizedMethod = sanitizer.sanitize(method).toUpperCase(Locale.ROOT);
        if (HTTP_METHODS.contains(normalizedMethod)) {
            return normalizedMethod;
        }
        return "UNKNOWN";
    }

    private boolean shouldLog(SecurityEventType eventType) {
        if (!HIGH_VOLUME_EVENTS.contains(eventType)) {
            return true;
        }

        long now = System.nanoTime();
        AtomicLong lastEvent = lastLoggedAt.computeIfAbsent(
                eventType,
                ignored -> new AtomicLong()
        );
        long previous = lastEvent.get();
        if (previous == 0L) {
            return lastEvent.compareAndSet(0L, now);
        }
        return now - previous >= HIGH_VOLUME_LOG_INTERVAL_NANOS
                && lastEvent.compareAndSet(previous, now);
    }

    private boolean isWarningEvent(
            SecurityEventType eventType,
            SecurityEventOutcome outcome
    ) {
        if (outcome == SecurityEventOutcome.FAILURE) {
            return true;
        }
        return eventType == SecurityEventType.CSRF_REJECTED
                || eventType == SecurityEventType.CORS_REJECTED
                || eventType == SecurityEventType.RATE_LIMIT_REJECTED
                || eventType == SecurityEventType.AUTH_RATE_LIMITED
                || eventType == SecurityEventType.REGISTER_RATE_LIMITED
                || eventType == SecurityEventType.RESOURCE_SATURATION
                || eventType == SecurityEventType.PROVIDER_FAILURE;
    }
}
