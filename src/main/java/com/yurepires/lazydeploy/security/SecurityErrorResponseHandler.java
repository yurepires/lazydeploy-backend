package com.yurepires.lazydeploy.security;

import com.yurepires.lazydeploy.exception.ProblemDetailFactory;
import com.yurepires.lazydeploy.service.observability.LogSanitizer;
import com.yurepires.lazydeploy.service.observability.SecurityEventLogger;
import com.yurepires.lazydeploy.service.observability.SecurityEventOutcome;
import com.yurepires.lazydeploy.service.observability.SecurityEventType;
import com.yurepires.lazydeploy.service.observability.SecurityMetrics;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
public class SecurityErrorResponseHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final SecurityMetrics securityMetrics;
    private final SecurityEventLogger securityEventLogger;

    public SecurityErrorResponseHandler(ObjectMapper objectMapper) {
        this(
                objectMapper,
                new SecurityMetrics(null),
                new SecurityEventLogger(new LogSanitizer())
        );
    }

    @Autowired
    public SecurityErrorResponseHandler(
            ObjectMapper objectMapper,
            SecurityMetrics securityMetrics,
            SecurityEventLogger securityEventLogger
    ) {
        this.objectMapper = objectMapper;
        this.securityMetrics = securityMetrics;
        this.securityEventLogger = securityEventLogger;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException {
        securityMetrics.recordHttpResponse(401, "authentication");
        securityMetrics.recordAuthorizationRejection("unauthenticated");
        securityEventLogger.log(
                SecurityEventType.UNAUTHORIZED_REQUEST,
                SecurityEventOutcome.REJECTED,
                "UNAUTHENTICATED",
                request.getRequestURI()
        );
        writeResponse(
                response,
                HttpStatus.UNAUTHORIZED,
                "Authentication required",
                "UNAUTHENTICATED",
                "Sessão inexistente ou expirada",
                request.getRequestURI()
        );
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException exception
    ) throws IOException {
        String errorCode = "ACCESS_DENIED";
        String title = "Access denied";
        String message = "Acesso negado";
        if (exception instanceof CsrfException) {
            errorCode = "CSRF_VALIDATION_FAILED";
            title = "Request validation failed";
            message = "Não foi possível validar a requisição.";
            securityMetrics.recordCsrfRejection(request.getRequestURI());
            securityMetrics.recordHttpResponse(403, "csrf");
            securityEventLogger.log(
                    SecurityEventType.CSRF_REJECTED,
                    SecurityEventOutcome.REJECTED,
                    "CSRF_VALIDATION_FAILED",
                    request.getRequestURI(),
                    request.getMethod()
            );
        } else {
            securityMetrics.recordHttpResponse(403, "authorization");
            securityMetrics.recordAuthorizationRejection("forbidden");
            securityEventLogger.log(
                    SecurityEventType.ACCESS_DENIED,
                    SecurityEventOutcome.REJECTED,
                    "FORBIDDEN",
                    request.getRequestURI()
            );
        }

        writeResponse(
                response,
                HttpStatus.FORBIDDEN,
                title,
                errorCode,
                message,
                request.getRequestURI()
        );
    }

    private void writeResponse(
            HttpServletResponse response,
            HttpStatus status,
            String title,
            String code,
            String message,
            String path
    ) throws IOException {
        ProblemDetail problemDetail = ProblemDetailFactory.create(
                status,
                title,
                message,
                code,
                path,
                java.util.List.of()
        );
        response.setStatus(status.value());
        response.setContentType("application/problem+json");
        response.getWriter().write(objectMapper.writeValueAsString(problemDetail));
    }
}
