package com.yurepires.lazydeploy.dto.response;

public record FieldValidationErrorResponse(
        String field,
        String message
) {}
