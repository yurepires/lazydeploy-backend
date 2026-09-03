package com.yurepires.lazydeploy.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record PatchRuleRequest(
        @NotNull Boolean enabled,
        String type,
        Map<String, Object> parameters
) {

    public PatchRuleRequest {
        if (parameters != null) {
            parameters = Collections.unmodifiableMap(
                    new LinkedHashMap<>(parameters)
            );
        }
    }

    public PatchRuleRequest(Boolean enabled) {
        this(enabled, null, null);
    }
}
