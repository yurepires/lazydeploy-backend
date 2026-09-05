package com.yurepires.lazydeploy.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record NotificationChannelRequest(
        @NotBlank @Size(max = 64) String type,
        boolean enabled,
        Map<String, Object> parameters,
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String recipient
) {
    public NotificationChannelRequest(
            String type,
            boolean enabled,
            Map<String, Object> parameters
    ) {
        this(type, enabled, parameters, null);
    }

    public NotificationChannelRequest {
        if (parameters == null) {
            parameters = Map.of();
        } else {
            parameters = Collections.unmodifiableMap(
                    new LinkedHashMap<>(parameters)
            );
        }
    }
}
