package com.yurepires.lazydeploy.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Canal criado junto com uma nova subscription.
 */
public record ConfigureChannelRequest(
        @NotBlank @Size(max = 64) String type,
        Boolean enabled,
        Map<String, Object> parameters,
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String recipient
) {

    public ConfigureChannelRequest(
            String type,
            Boolean enabled,
            Map<String, Object> parameters
    ) {
        this(type, enabled, parameters, null);
    }

    public ConfigureChannelRequest {
        if (enabled == null) {
            enabled = Boolean.TRUE;
        }

        if (parameters == null) {
            parameters = Map.of();
        } else {
            parameters = Collections.unmodifiableMap(
                    new LinkedHashMap<>(parameters)
            );
        }
    }
}
