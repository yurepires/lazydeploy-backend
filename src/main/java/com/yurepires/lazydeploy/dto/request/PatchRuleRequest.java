package com.yurepires.lazydeploy.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record PatchRuleRequest(
        @NotNull Boolean enabled,
        @Size(max = 64) String type,
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
