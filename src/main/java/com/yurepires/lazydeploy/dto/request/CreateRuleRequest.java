package com.yurepires.lazydeploy.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record CreateRuleRequest(
        @NotBlank String type,
        Boolean enabled,
        @NotNull Map<String, Object> parameters
) {

    public CreateRuleRequest {
        if (enabled == null) {
            enabled = Boolean.TRUE;
        }

        if (parameters != null) {
            parameters = Collections.unmodifiableMap(
                    new LinkedHashMap<>(parameters)
            );
        }
    }
}
