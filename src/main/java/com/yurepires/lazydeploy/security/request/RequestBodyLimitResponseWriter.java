package com.yurepires.lazydeploy.security.request;

import com.yurepires.lazydeploy.exception.ProblemDetailFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class RequestBodyLimitResponseWriter {

    private final ObjectMapper objectMapper;

    public RequestBodyLimitResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        ProblemDetail problemDetail = ProblemDetailFactory.create(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "Payload Too Large",
                "O corpo da requisição excede o tamanho máximo permitido.",
                "REQUEST_TOO_LARGE",
                request.getRequestURI(),
                List.of()
        );

        response.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
        response.setContentType("application/problem+json");
        byte[] responseBody = objectMapper.writeValueAsString(problemDetail)
                .getBytes(StandardCharsets.UTF_8);
        response.getOutputStream().write(responseBody);
    }
}
