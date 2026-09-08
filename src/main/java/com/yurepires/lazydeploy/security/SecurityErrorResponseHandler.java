package com.yurepires.lazydeploy.security;

import com.yurepires.lazydeploy.exception.ProblemDetailFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

    public SecurityErrorResponseHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException {
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
