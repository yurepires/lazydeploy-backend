package com.yurepires.lazydeploy.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/** Adiciona uma correlação opaca e limitada a cada requisição HTTP. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    public static final String MDC_KEY = "correlationId";

    private static final Pattern VALID_REQUEST_ID = Pattern.compile(
            "[A-Za-z0-9_-]{1,100}"
    );

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String previousCorrelationId = MDC.get(MDC_KEY);
        String correlationId = resolveCorrelationId(request.getHeader(REQUEST_ID_HEADER));
        MDC.put(MDC_KEY, correlationId);
        response.setHeader(REQUEST_ID_HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            restorePreviousCorrelationId(previousCorrelationId);
        }
    }

    public String resolveCorrelationId(String candidate) {
        if (candidate != null && VALID_REQUEST_ID.matcher(candidate).matches()) {
            return candidate;
        }
        return UUID.randomUUID().toString();
    }

    private void restorePreviousCorrelationId(String previousCorrelationId) {
        if (previousCorrelationId == null) {
            MDC.remove(MDC_KEY);
            return;
        }
        MDC.put(MDC_KEY, previousCorrelationId);
    }
}
