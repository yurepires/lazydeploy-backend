package com.yurepires.lazydeploy.exception;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.time.Instant;
import java.util.List;

public final class ProblemDetailFactory {

    private ProblemDetailFactory() {
    }

    public static ProblemDetail create(
            HttpStatus status,
            String title,
            String detail,
            String errorCode,
            String path,
            List<FieldValidationError> fieldErrors
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setType(URI.create("about:blank"));
        problemDetail.setTitle(title);
        problemDetail.setInstance(URI.create(path));
        problemDetail.setProperty("errorCode", errorCode);
        problemDetail.setProperty("code", errorCode);
        problemDetail.setProperty("timestamp", Instant.now());

        String correlationId = MDC.get("correlationId");
        if (correlationId != null && !correlationId.isBlank()) {
            problemDetail.setProperty("correlationId", correlationId);
        }

        if (fieldErrors != null && !fieldErrors.isEmpty()) {
            problemDetail.setProperty("fieldErrors", fieldErrors);
        }

        return problemDetail;
    }

    public record FieldValidationError(String field, String message) {
    }
}
