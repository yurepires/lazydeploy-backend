package com.yurepires.lazydeploy.security;

import com.yurepires.lazydeploy.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

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
                HttpServletResponse.SC_UNAUTHORIZED,
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
        writeResponse(
                response,
                HttpServletResponse.SC_FORBIDDEN,
                "ACCESS_DENIED",
                "Acesso negado ou token CSRF inválido",
                request.getRequestURI()
        );
    }

    private void writeResponse(
            HttpServletResponse response,
            int status,
            String code,
            String message,
            String path
    ) throws IOException {
        String reason = "Unauthorized";
        if (status == HttpServletResponse.SC_FORBIDDEN) {
            reason = "Forbidden";
        }

        ErrorResponse errorResponse = new ErrorResponse(
                Instant.now(),
                status,
                reason,
                code,
                message,
                path,
                List.of()
        );
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
