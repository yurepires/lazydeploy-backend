package com.yurepires.lazydeploy.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record NotificationRuleRequest(
        @NotBlank String type,
        boolean enabled,
        Map<String, Object> parameters
) {
    public NotificationRuleRequest {
        if (parameters == null) {
            parameters = Map.of();
        } else {
            parameters = Collections.unmodifiableMap(
                    new LinkedHashMap<>(parameters)
            );
        }
    }
}
