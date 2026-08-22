package com.yurepires.lazydeploy.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record NotificationChannelRequest(
        @NotBlank String type,
        boolean enabled,
        Map<String, Object> parameters
) {
    public NotificationChannelRequest {
        if (parameters == null) {
            parameters = Map.of();
        } else {
            parameters = Map.copyOf(parameters);
        }
    }
}
