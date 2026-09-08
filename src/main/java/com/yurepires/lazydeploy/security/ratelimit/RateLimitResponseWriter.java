package com.yurepires.lazydeploy.security.ratelimit;

import com.yurepires.lazydeploy.exception.ProblemDetailFactory;
import com.yurepires.lazydeploy.exception.RateLimitExceededException;
import com.yurepires.lazydeploy.service.observability.SecurityMetrics;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class RateLimitResponseWriter {

    private final ObjectMapper objectMapper;
    private final SecurityMetrics securityMetrics;

    public RateLimitResponseWriter(ObjectMapper objectMapper) {
        this(objectMapper, new SecurityMetrics(null));
    }

    @Autowired
    public RateLimitResponseWriter(
            ObjectMapper objectMapper,
            SecurityMetrics securityMetrics
    ) {
        this.objectMapper = objectMapper;
        this.securityMetrics = securityMetrics;
    }

    public void write(
            HttpServletRequest request,
            HttpServletResponse response,
            RateLimitExceededException exception
    ) throws IOException {
        securityMetrics.recordHttpResponse(429, "rate_limit");
        ProblemDetail problemDetail = ProblemDetailFactory.create(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too Many Requests",
                exception.getMessage(),
                exception.getErrorCode(),
                request.getRequestURI(),
                List.of()
        );

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/problem+json");
        response.setHeader(
                "Retry-After",
                String.valueOf(exception.getRetryAfterSeconds())
        );
        byte[] responseBody = objectMapper.writeValueAsString(problemDetail)
                .getBytes(StandardCharsets.UTF_8);
        response.getOutputStream().write(responseBody);
    }
}
