package com.yurepires.lazydeploy.dto.response;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        List<FieldValidationErrorResponse> validationErrors
) {
    public ErrorResponse {
        if (validationErrors == null) {
            validationErrors = List.of();
        } else {
            validationErrors = List.copyOf(validationErrors);
        }
    }
}
