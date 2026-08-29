package com.yurepires.lazydeploy.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record CreateChannelRequest(
        @NotBlank String type,
        Boolean enabled,
        Map<String, Object> parameters,
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String recipient
) {

    public CreateChannelRequest(
            String type,
            Boolean enabled,
            Map<String, Object> parameters
    ) {
        this(type, enabled, parameters, null);
    }

    public CreateChannelRequest {
        if (enabled == null) {
            enabled = Boolean.TRUE;
        }

        if (parameters == null) {
            parameters = Map.of();
        } else {
            parameters = Collections.unmodifiableMap(new LinkedHashMap<>(parameters));
        }
    }
}
