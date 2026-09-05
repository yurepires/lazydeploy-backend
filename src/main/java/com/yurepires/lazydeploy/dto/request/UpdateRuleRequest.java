package com.yurepires.lazydeploy.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record UpdateRuleRequest(
        @NotBlank @Size(max = 64) String type,
        Boolean enabled,
        @NotNull Map<String, Object> parameters
) {

    public UpdateRuleRequest {
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
