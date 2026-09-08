package com.yurepires.lazydeploy.security;

import com.yurepires.lazydeploy.config.CorsProperties;
import com.yurepires.lazydeploy.service.observability.SecurityEventLogger;
import com.yurepires.lazydeploy.service.observability.SecurityEventOutcome;
import com.yurepires.lazydeploy.service.observability.SecurityEventType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

/** Registra rejeições CORS de forma limitada para evitar amplificação de logs. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class CorsRejectionObservabilityFilter extends OncePerRequestFilter {

    private static final long LOG_INTERVAL_NANOS = 10_000_000_000L;

    private final CorsProperties properties;
    private final SecurityEventLogger securityEventLogger;
    private final AtomicLong lastLoggedAt = new AtomicLong();

    public CorsRejectionObservabilityFilter(
            CorsProperties properties,
            SecurityEventLogger securityEventLogger
    ) {
        this.properties = properties;
        this.securityEventLogger = securityEventLogger;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String origin = request.getHeader("Origin");
        if (isRejectedApiOrigin(request, origin) && shouldLog()) {
            securityEventLogger.log(
                    SecurityEventType.CORS_REJECTED,
                    SecurityEventOutcome.REJECTED,
                    "ORIGIN_NOT_ALLOWED",
                    request.getRequestURI(),
                    request.getMethod()
            );
        }
        filterChain.doFilter(request, response);
    }

    private boolean isRejectedApiOrigin(
            HttpServletRequest request,
            String origin
    ) {
        return origin != null
                && request.getRequestURI() != null
                && request.getRequestURI().startsWith(request.getContextPath() + "/api/")
                && !properties.allowedOrigins().contains("*")
                && !properties.allowedOrigins().contains(origin);
    }

    private boolean shouldLog() {
        long now = System.nanoTime();
        long previous = lastLoggedAt.get();
        if (previous == 0L) {
            return lastLoggedAt.compareAndSet(0L, now);
        }
        return now - previous >= LOG_INTERVAL_NANOS
                && lastLoggedAt.compareAndSet(previous, now);
    }
}
